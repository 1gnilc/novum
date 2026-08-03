package com.gnilc.auth.authz.rbac.event;

import lombok.Data;

import java.util.Objects;

/**
 * 菜单数据变化事件。
 */
@Data
public final class MenuEvent {
    private final Action action;
    private final Long menuId;

    /**
     * 创建菜单数据变化事件。
     *
     * @param action 菜单变更动作
     * @param menuId 菜单 ID
     */
    public MenuEvent(Action action, Long menuId) {
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.menuId = Objects.requireNonNull(menuId, "menuId must not be null");
    }

    public enum Action {
        CREATE,
        UPDATE,
        DELETE
    }
}
