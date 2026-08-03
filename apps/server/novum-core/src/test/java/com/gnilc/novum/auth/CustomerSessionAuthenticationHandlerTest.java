package com.gnilc.novum.auth;

import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.session.CustomerSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerSessionAuthenticationHandlerTest {
    private final CustomerSessionManager sessions = mock(CustomerSessionManager.class);
    private final CustomerSessionAuthenticationHandler handler =
            new CustomerSessionAuthenticationHandler(sessions, messages(), AuthLocaleTestSupport.localeResolver());

    @Test
    void supportsOnlyCustomerSessionTokens() {
        when(sessions.supportsAccessToken("customer.7.value")).thenReturn(true);

        assertThat(handler.supports(context("Bearer customer.7.value"))).isTrue();
        assertThat(handler.supports(context("Bearer sys_admin.7.value"))).isFalse();
        assertThat(handler.supports(context("Basic customer.7.value"))).isFalse();
    }

    @Test
    void validCustomerTokenCreatesTheGlobalRbacPrincipal() {
        MockHttpServletRequest request = request("Bearer customer.7.valid");
        when(sessions.validateAccessToken("customer.7.valid")).thenReturn(84L);

        AuthenticationResult result = handler.authenticate(context(request));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal().getIdentifier()).isEqualTo("84");
    }

    @Test
    void invalidCustomerTokenUsesTheRequestLocale() {
        MockHttpServletRequest request = request("Bearer customer.7.invalid");
        request.addHeader("Accept-Language", "zh-CN");
        request.addPreferredLocale(Locale.SIMPLIFIED_CHINESE);
        when(sessions.validateAccessToken("customer.7.invalid")).thenReturn(null);

        AuthenticationResult result = handler.authenticate(context(request));

        assertThat(result.isAuthenticated()).isFalse();
        assertThat(result.getReason()).isEqualTo("访问令牌无效或已过期。");
    }

    private ServletAuthenticationContext context(String authorization) {
        return context(request(authorization));
    }

    private ServletAuthenticationContext context(MockHttpServletRequest request) {
        return new ServletAuthenticationContext(request, new MockHttpServletResponse());
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }

    private static I18nMessageService messages() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("system.auth.accessToken.invalid", Locale.US,
                "The access token is invalid or has expired.");
        source.addMessage("system.auth.accessToken.invalid", Locale.SIMPLIFIED_CHINESE,
                "访问令牌无效或已过期。");
        return new I18nMessageService(source, "en-US");
    }
}
