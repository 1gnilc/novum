package com.gnilc.system.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.common.i18n.I18nMessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminServletAuthenticationFailureHandlerTest {

    @Test
    void internalAuthenticationErrorReturnsLocalizedGenericFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "zh-CN");
        request.addPreferredLocale(Locale.SIMPLIFIED_CHINESE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletAuthenticationContext context = new ServletAuthenticationContext(request, response);

        new AdminServletAuthenticationFailureHandler(messages(), AuthLocaleTestSupport.localeResolver()).handle(
                context,
                AuthenticationResult.failed(null, new IllegalStateException("backend unavailable")));

        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(body.get("code").asInt()).isEqualTo(20002);
        assertThat(body.get("data").isNull()).isTrue();
        assertThat(body.get("error").asText()).isEqualTo("认证失败。");
        assertThat(body.get("message").asText()).isEqualTo("认证失败。");
    }

    @Test
    void missingLanguageHeaderUsesConfiguredEnglishInsteadOfTheServerLocale() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Accept-Language")).thenReturn(null);
        when(request.getLocale()).thenReturn(Locale.SIMPLIFIED_CHINESE);
        when(request.getLocales()).thenReturn(Collections.enumeration(List.of(Locale.SIMPLIFIED_CHINESE)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AdminServletAuthenticationFailureHandler(messages(), AuthLocaleTestSupport.localeResolver()).handle(
                new ServletAuthenticationContext(request, response),
                AuthenticationResult.failed(null));

        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        assertThat(body.get("error").asText()).isEqualTo("Authentication failed.");
        assertThat(body.get("message").asText()).isEqualTo("Authentication failed.");
    }

    private static I18nMessageService messages() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("system.auth.authentication.failed", Locale.US, "Authentication failed.");
        source.addMessage("system.auth.authentication.failed", Locale.SIMPLIFIED_CHINESE, "认证失败。");
        return new I18nMessageService(source, "en-US");
    }
}
