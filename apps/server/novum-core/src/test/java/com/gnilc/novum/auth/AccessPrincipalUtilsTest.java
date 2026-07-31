package com.gnilc.novum.auth;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessPrincipalUtilsTest {
    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getUserIdReadsTheCurrentAccessPrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(42L));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(AccessPrincipalUtils.getUserId()).isEqualTo(42L);
    }

    @Test
    void getUserIdRejectsAnUnauthenticatedRequest() {
        assertThatThrownBy(AccessPrincipalUtils::getUserId)
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Your session is no longer valid. Sign in again.");
    }
}
