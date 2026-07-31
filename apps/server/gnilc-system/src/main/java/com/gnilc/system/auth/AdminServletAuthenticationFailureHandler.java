package com.gnilc.system.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationFailureHandler;
import com.gnilc.common.constant.ResponseCode;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.R;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 后台管理员 Servlet 认证失败响应。
 */
@Component
public class AdminServletAuthenticationFailureHandler implements ServletAuthenticationFailureHandler {
    private static final String DEFAULT_MESSAGE_CODE = "system.auth.authentication.failed";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final I18nMessageService messages;
    private final LocaleResolver localeResolver;

    public AdminServletAuthenticationFailureHandler(
            I18nMessageService messages,
            LocaleResolver localeResolver) {
        this.messages = messages;
        this.localeResolver = localeResolver;
    }

    @Override
    public void handle(ServletAuthenticationContext context, AuthenticationResult result) {
        HttpServletResponse response = context.getResponse();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(JSON_CONTENT_TYPE);
        try {
            String message = resolveMessage(context, result);
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                    R.error(ResponseCode.UNAUTHORIZED, message)));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write authentication failure response", exception);
        }
    }

    private String resolveMessage(ServletAuthenticationContext context, AuthenticationResult result) {
        if (result != null && StringUtils.hasText(result.getReason())) {
            return result.getReason();
        }
        return messages.get(
                DEFAULT_MESSAGE_CODE,
                localeResolver.resolveLocale(context.getRequest()));
    }
}
