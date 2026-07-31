package com.gnilc.novum.admin.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import com.google.common.util.concurrent.Striped;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

/**
 * 管理员高频查询缓存的统一入口。
 */
@Slf4j
@Service
public class AdminCacheService {
    static final String USER_INFO_PREFIX = "sys:admin:user-info:";
    static final String ROLE_CODES_PREFIX = "sys:admin:role-codes:";
    static final String MENU_ACCESS_CODES_PREFIX = "sys:admin:menu-access-codes:";
    static final String MENU_ROUTES_PREFIX = "sys:admin:menu-routes:";

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration SECOND_DELETE_DELAY = Duration.ofSeconds(5);
    private static final int SCAN_COUNT = 1000;
    private static final int DELETE_BATCH_SIZE = 500;
    private static final TypeReference<AdminVo> ADMIN_INFO_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<MenuRouteVo>> MENU_ROUTE_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Striped<Lock> loadLocks = Striped.lock(256);
    private final ScheduledExecutorService deleteExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "admin-cache-executor");
        thread.setDaemon(true);
        return thread;
    });

    public AdminCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public AdminVo getUserInfo(Long userId, Supplier<AdminVo> loader) {
        return getOrLoad(userInfoKey(userId), ADMIN_INFO_TYPE, loader);
    }

    public List<String> getRoleCodes(Long userId, Supplier<List<String>> loader) {
        return getOrLoad(roleCodesKey(userId), STRING_LIST_TYPE, loader);
    }

    public List<String> getMenuAccessCodes(Long userId, Supplier<List<String>> loader) {
        return getOrLoad(menuAccessCodesKey(userId), STRING_LIST_TYPE, loader);
    }

    public List<MenuRouteVo> getMenuRoutes(Long userId, Supplier<List<MenuRouteVo>> loader) {
        return getOrLoad(menuRoutesKey(userId), MENU_ROUTE_LIST_TYPE, loader);
    }

    public void removeUserInfo(Long userId) {
        redisTemplate.delete(userInfoKey(userId));
    }

    public void removeRoleCodes(Long userId) {
        redisTemplate.delete(roleCodesKey(userId));
    }

    public void removeMenuAccessCodes(Long userId) {
        redisTemplate.delete(menuAccessCodesKey(userId));
    }

    public void removeMenuRoutes(Long userId) {
        redisTemplate.delete(menuRoutesKey(userId));
    }

    public void removeAllRoleCodes() {
        removeKeysByPrefix(ROLE_CODES_PREFIX);
    }

    public void removeAllMenuAccessCodes() {
        removeKeysByPrefix(MENU_ACCESS_CODES_PREFIX);
    }

    public void removeAllMenuRoutes() {
        removeKeysByPrefix(MENU_ROUTES_PREFIX);
    }

    void scheduleSecondDelete(Runnable action) {
        Objects.requireNonNull(action, "action must not be null");
        deleteExecutor.schedule(() -> {
            try {
                action.run();
            } catch (RuntimeException exception) {
                log.error("Failed to perform the delayed Admin cache deletion.", exception);
            }
        }, SECOND_DELETE_DELAY.toMillis(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdownDeleteExecutor() {
        deleteExecutor.shutdown();
    }

    static String userInfoKey(Long userId) {
        return USER_INFO_PREFIX + requireUserId(userId);
    }

    static String roleCodesKey(Long userId) {
        return ROLE_CODES_PREFIX + requireUserId(userId);
    }

    static String menuAccessCodesKey(Long userId) {
        return MENU_ACCESS_CODES_PREFIX + requireUserId(userId);
    }

    static String menuRoutesKey(Long userId) {
        return MENU_ROUTES_PREFIX + requireUserId(userId);
    }

    private <T> T getOrLoad(String key, TypeReference<T> type, Supplier<T> loader) {
        Objects.requireNonNull(loader, "loader must not be null");
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return deserialize(key, cached, type);
        }

        Lock lock = loadLocks.get(key);
        lock.lock();
        try {
            cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return deserialize(key, cached, type);
            }
            T loaded = loader.get();
            if (loaded != null) {
                redisTemplate.opsForValue().set(key, serialize(key, loaded), CACHE_TTL);
            }
            return loaded;
        } finally {
            lock.unlock();
        }
    }

    private <T> T deserialize(String key, String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize Admin cache key " + key, exception);
        }
    }

    private String serialize(String key, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Admin cache key " + key, exception);
        }
    }

    private void removeKeysByPrefix(String prefix) {
        RedisConnectionFactory connectionFactory = Objects.requireNonNull(
                redisTemplate.getConnectionFactory(),
                "Redis connection factory must not be null");
        ScanOptions options = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(SCAN_COUNT)
                .build();
        try (RedisConnection connection = connectionFactory.getConnection();
             Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            List<byte[]> keys = new ArrayList<>(DELETE_BATCH_SIZE);
            while (cursor.hasNext()) {
                keys.add(cursor.next());
                if (keys.size() == DELETE_BATCH_SIZE) {
                    removeBatch(connection, keys);
                    keys.clear();
                }
            }
            if (!keys.isEmpty()) {
                removeBatch(connection, keys);
            }
        }
    }

    private void removeBatch(RedisConnection connection, List<byte[]> keys) {
        connection.keyCommands().del(keys.toArray(byte[][]::new));
    }

    private static Long requireUserId(Long userId) {
        return Objects.requireNonNull(userId, "userId must not be null");
    }
}
