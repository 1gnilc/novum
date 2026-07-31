package com.gnilc.novum.admin.event;

import java.util.Objects;

/**
 * 管理员数据变化事件。
 *
 * @param action 管理员变更动作
 * @param userId RBAC 用户 ID
 */
public record AdminEvent(Action action, Long userId) {
    public AdminEvent {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
    }

    public enum Action {
        CREATE,
        UPDATE,
        DELETE
    }
}
