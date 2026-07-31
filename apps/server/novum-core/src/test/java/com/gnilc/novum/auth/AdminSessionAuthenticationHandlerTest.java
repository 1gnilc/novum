package com.gnilc.novum.auth;

import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.session.AdminSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminSessionAuthenticationHandlerTest {
    private final AdminSessionManager sessions = mock(AdminSessionManager.class);
    private final AdminSessionAuthenticationHandler handler =
            new AdminSessionAuthenticationHandler(sessions, messages(), AuthLocaleTestSupport.localeResolver());

    @Test
    void bearerTokenSupportIsNamespacedBySessionManager() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        when(sessions.supportsAccessToken("token")).thenReturn(true);

        assertThat(handler.supports(context(request))).isTrue();

        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic token");
        assertThat(handler.supports(context(request))).isFalse();
    }

    @Test
    void validBearerTokenCreatesPrincipalAndInvalidTokenFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid");
        when(sessions.validateAccessToken("valid")).thenReturn(12L);

        AuthenticationResult valid = handler.authenticate(context(request));

        assertThat(valid.isAuthenticated()).isTrue();
        assertThat(valid.getPrincipal().getIdentifier()).isEqualTo("12");

        request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "zh-CN");
        request.addPreferredLocale(Locale.SIMPLIFIED_CHINESE);
        request.addHeader("Authorization", "Bearer invalid");
        when(sessions.validateAccessToken("invalid")).thenReturn(null);
        AuthenticationResult invalid = handler.authenticate(context(request));
        assertThat(invalid.isAuthenticated()).isFalse();
        assertThat(invalid.getReason()).isEqualTo("访问令牌无效或已过期。");
    }

    @Test
    void invalidTokenUsesTheFirstSupportedLanguagePreference() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "fr-FR, zh-CN;q=0.9");
        request.setPreferredLocales(List.of(Locale.FRANCE, Locale.SIMPLIFIED_CHINESE));
        request.addHeader("Authorization", "Bearer invalid");
        when(sessions.validateAccessToken("invalid")).thenReturn(null);

        AuthenticationResult invalid = handler.authenticate(context(request));

        assertThat(invalid.getReason()).isEqualTo("访问令牌无效或已过期。");
    }

    private ServletAuthenticationContext context(MockHttpServletRequest request) {
        return new ServletAuthenticationContext(request, new MockHttpServletResponse());
    }

    private static I18nMessageService messages() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage(
                "system.auth.accessToken.invalid",
                Locale.US,
                "The access token is invalid or has expired.");
        source.addMessage(
                "system.auth.accessToken.invalid",
                Locale.SIMPLIFIED_CHINESE,
                "访问令牌无效或已过期。");
        return new I18nMessageService(source, "en-US");
    }
}
