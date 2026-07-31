package com.gnilc.novum.auth;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessIdentity;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.denied.AccessDeniedContext;
import com.gnilc.auth.authz.servlet.context.ServletAccessDeniedContext;
import com.gnilc.common.i18n.I18nMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultServletAccessDeniedHandlerTest {
    @Test
    void writesJson403OnlyForOpenServletResponse() throws Exception {
        DefaultServletAccessDeniedHandler handler = new DefaultServletAccessDeniedHandler(
                messages(), AuthLocaleTestSupport.localeResolver());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "zh-CN");
        request.addPreferredLocale(Locale.SIMPLIFIED_CHINESE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletAccessDeniedContext deniedContext = new ServletAccessDeniedContext(
                request, response, (req, res) -> { });
        AccessContext access = new AccessContext(
                new AccessIdentity("1", Map.of()), new AccessTarget("/private", "GET"));

        assertThat(handler.supports(access, deniedContext)).isTrue();
        handler.handle(access, deniedContext);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"code\":20003", "\"error\":\"访问被拒绝。\"");
        assertThat(handler.supports(access, new AccessDeniedContext() { })).isFalse();
    }

    private static I18nMessageService messages() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("system.auth.access.denied", Locale.US, "Access denied.");
        source.addMessage("system.auth.access.denied", Locale.SIMPLIFIED_CHINESE, "访问被拒绝。");
        return new I18nMessageService(source, "en-US");
    }
}
