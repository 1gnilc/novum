package com.gnilc.novum.admin.cache;

import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import com.gnilc.auth.authz.rbac.event.MenuEvent;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.novum.admin.event.AdminEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 将管理员与 RBAC 数据变化事件映射为管理员查询缓存删除动作。
 */
@Component
public class AdminCacheEventListener {
    private final AdminCacheService cacheService;
    private final UserRoleService userRoleService;

    public AdminCacheEventListener(AdminCacheService cacheService, UserRoleService userRoleService) {
        this.cacheService = cacheService;
        this.userRoleService = userRoleService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void handleAdmin(AdminEvent event) {
        if (event != null) {
            reset(() -> cacheService.removeUserInfo(event.userId()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void handleMenu(MenuEvent event) {
        if (event != null) {
            reset(() -> {
                cacheService.removeAllMenuAccessCodes();
                cacheService.removeAllMenuRoutes();
            });
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void handleAuthorization(AuthorizationEvent<Long> event) {
        if (event == null || event.getType() == null) {
            return;
        }
        switch (event.getType()) {
            case ROLE -> resetRoleCodes(usersForRole(event.getData()));
            case ROLE_MENU -> resetMenuAccessCodesAndRoutes(usersForRole(event.getData()));
            case USER, USER_ROLE -> resetUserAuthorization(event.getData());
            case PERMISSION, ROLE_PERMISSION, ALL -> {
                // These event types do not change Admin query results with Long payloads.
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void handleAllAuthorization(AuthorizationEvent<Void> event) {
        if (event != null && event.getType() == AuthorizationEvent.Type.ALL) {
            reset(() -> {
                cacheService.removeAllRoleCodes();
                cacheService.removeAllMenuAccessCodes();
                cacheService.removeAllMenuRoutes();
            });
        }
    }

    private void resetRoleCodes(List<Long> userIds) {
        if (!CollectionUtils.isEmpty(userIds)) {
            List<Long> affectedUserIds = userIds.stream().distinct().toList();
            reset(() -> affectedUserIds.forEach(cacheService::removeRoleCodes));
        }
    }

    private void resetMenuAccessCodesAndRoutes(List<Long> userIds) {
        if (!CollectionUtils.isEmpty(userIds)) {
            List<Long> affectedUserIds = userIds.stream().distinct().toList();
            reset(() -> affectedUserIds.forEach(userId -> {
                cacheService.removeMenuAccessCodes(userId);
                cacheService.removeMenuRoutes(userId);
            }));
        }
    }

    private void resetUserAuthorization(Long userId) {
        if (userId != null) {
            reset(() -> {
                cacheService.removeRoleCodes(userId);
                cacheService.removeMenuAccessCodes(userId);
                cacheService.removeMenuRoutes(userId);
            });
        }
    }

    private List<Long> usersForRole(Long roleId) {
        return roleId == null ? List.of() : userRoleService.getUserIds(roleId);
    }

    private void reset(Runnable resetAction) {
        resetAction.run();
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheService.scheduleSecondDelete(resetAction);
                }
            });
            return;
        }
        cacheService.scheduleSecondDelete(resetAction);
    }
}
