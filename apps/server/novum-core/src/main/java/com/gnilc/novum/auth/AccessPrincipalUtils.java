package com.gnilc.novum.auth;

import com.gnilc.auth.authn.context.AccessPrincipal;
import com.gnilc.auth.authn.servlet.context.DefaultAccessPrincipalHolder;
import com.gnilc.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

/**
 * 当前访问主体工具。
 */
public final class AccessPrincipalUtils {
    private AccessPrincipalUtils() {
    }

    /**
     * 获取当前认证用户 ID。
     */
    public static Long getUserId() {
        AccessPrincipal principal = DefaultAccessPrincipalHolder.getPrincipal();
        Preconditions.checkArgument(principal != null,
                "Your session is no longer valid. Sign in again.");
        String identifier = principal.getIdentifier();
        Preconditions.checkArgument(StringUtils.isNotBlank(identifier),
                "Your session is no longer valid. Sign in again.");
        return Long.valueOf(identifier);
    }
}
