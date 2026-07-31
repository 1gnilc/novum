package com.gnilc.novum.admin.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCacheServiceTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;

    private AdminCacheService cache;

    @BeforeEach
    void setUp() {
        cache = new AdminCacheService(redis, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        cache.shutdownDeleteExecutor();
    }

    @Test
    void cacheKeysAreStable() {
        assertThat(AdminCacheService.userInfoKey(41L)).isEqualTo("sys:admin:user-info:41");
        assertThat(AdminCacheService.roleCodesKey(41L)).isEqualTo("sys:admin:role-codes:41");
        assertThat(AdminCacheService.menuAccessCodesKey(41L))
                .isEqualTo("sys:admin:menu-access-codes:41");
        assertThat(AdminCacheService.menuRoutesKey(41L)).isEqualTo("sys:admin:menu-routes:41");
    }

    @Test
    void secondRedisCheckUsesValueFilledWhileWaitingForTheLocalLock() {
        when(redis.opsForValue()).thenReturn(values);
        String key = AdminCacheService.roleCodesKey(41L);
        when(values.get(key)).thenReturn(null, "[\"admin\"]");
        @SuppressWarnings("unchecked")
        Supplier<List<String>> loader = mock(Supplier.class);

        assertThat(cache.getRoleCodes(41L, loader)).containsExactly("admin");

        verify(values, never()).set(any(), any(), any(Duration.class));
        verifyNoInteractions(loader);
    }

    @Test
    void nullUserInfoIsNotCachedButEmptyListsAreCached() {
        when(redis.opsForValue()).thenReturn(values);
        String userInfoKey = AdminCacheService.userInfoKey(42L);
        String roleCodesKey = AdminCacheService.roleCodesKey(42L);
        when(values.get(userInfoKey)).thenReturn(null);
        when(values.get(roleCodesKey)).thenReturn(null);

        assertThat(cache.getUserInfo(42L, () -> null)).isNull();
        assertThat(cache.getRoleCodes(42L, List::of)).isEmpty();

        verify(values, never()).set(userInfoKey, "null", Duration.ofMinutes(30));
        verify(values).set(roleCodesKey, "[]", Duration.ofMinutes(30));
    }

    @Test
    void loaderFailureReleasesTheStripedLock() {
        when(redis.opsForValue()).thenReturn(values);
        String key = AdminCacheService.roleCodesKey(43L);
        when(values.get(key)).thenReturn(null);

        assertThatThrownBy(() -> cache.getRoleCodes(43L, () -> {
            throw new IllegalStateException("database unavailable");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        assertThat(cache.getRoleCodes(43L, () -> List.of("admin")))
                .containsExactly("admin");
        verify(values).set(key, "[\"admin\"]", Duration.ofMinutes(30));
    }

    @Test
    void malformedJsonFailsWithoutFallingBackToTheLoader() {
        when(redis.opsForValue()).thenReturn(values);
        String key = AdminCacheService.userInfoKey(44L);
        when(values.get(key)).thenReturn("not-json");
        @SuppressWarnings("unchecked")
        Supplier<AdminVo> loader = mock(Supplier.class);

        assertThatThrownBy(() -> cache.getUserInfo(44L, loader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(key);
        verifyNoInteractions(loader);
    }
}
