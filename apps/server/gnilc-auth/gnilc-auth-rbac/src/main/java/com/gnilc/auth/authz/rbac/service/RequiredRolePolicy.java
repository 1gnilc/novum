package com.gnilc.auth.authz.rbac.service;

/**
 * 判断用户角色绑定是否属于应用必需基线。
 */
@FunctionalInterface
public interface RequiredRolePolicy {
    boolean isRequired(Long userId, Long roleId);
}
