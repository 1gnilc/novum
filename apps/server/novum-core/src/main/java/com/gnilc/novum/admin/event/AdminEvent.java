package com.gnilc.novum.admin.event;

import lombok.Data;

import java.util.Objects;

/**
 * 管理员数据变化事件。
 */
@Data
public final class AdminEvent {
    private final Action action;
    private final Long userId;

    /**
     * 创建管理员数据变化事件。
     *
     * @param action 管理员变更动作
     * @param userId RBAC 用户 ID
     */
    public AdminEvent(Action action, Long userId) {
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
    }

    public enum Action {
        CREATE,
        UPDATE,
        DELETE
    }
}
