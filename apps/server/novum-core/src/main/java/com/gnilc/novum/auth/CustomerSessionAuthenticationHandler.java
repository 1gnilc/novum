package com.gnilc.novum.auth;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authn.handler.AuthenticationResult;
import com.gnilc.auth.authn.servlet.context.ServletAuthenticationContext;
import com.gnilc.auth.authn.servlet.handler.ServletAuthenticationHandler;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.session.CustomerSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 认证 Customer Bearer 访问令牌。
 */
@Component
public class CustomerSessionAuthenticationHandler implements ServletAuthenticationHandler {
    private static final String INVALID_ACCESS_TOKEN_MESSAGE = "system.auth.accessToken.invalid";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Pattern BEARER_VALUE_PATTERN = Pattern.compile("^Bearer\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("^Bearer\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);

    private final CustomerSessionManager sessionManager;
    private final I18nMessageService messages;
    private final LocaleResolver localeResolver;

    public CustomerSessionAuthenticationHandler(
            CustomerSessionManager sessionManager,
            I18nMessageService messages,
            LocaleResolver localeResolver) {
        this.sessionManager = sessionManager;
        this.messages = messages;
        this.localeResolver = localeResolver;
    }

    @Override
    public boolean supports(ServletAuthenticationContext context) {
        String credentials = resolveBearerCredentials(context.getRequest());
        return credentials != null && sessionManager.supportsAccessToken(credentials.trim());
    }

    @Override
    public AuthenticationResult authenticate(ServletAuthenticationContext context) {
        String accessToken = resolveBearerToken(context.getRequest());
        if (accessToken == null) {
            return invalidAccessToken(context);
        }
        Long userId = sessionManager.validateAccessToken(accessToken);
        return userId == null
                ? invalidAccessToken(context)
                : AuthenticationResult.authenticated(DefaultAccessPrincipal.of(userId));
    }

    private AuthenticationResult invalidAccessToken(ServletAuthenticationContext context) {
        return AuthenticationResult.failed(messages.get(
                INVALID_ACCESS_TOKEN_MESSAGE,
                localeResolver.resolveLocale(context.getRequest())));
    }

    private String resolveBearerCredentials(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null) {
            return null;
        }
        Matcher matcher = BEARER_VALUE_PATTERN.matcher(authorization);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null) {
            return null;
        }
        Matcher matcher = BEARER_TOKEN_PATTERN.matcher(authorization);
        return matcher.matches() ? matcher.group(1) : null;
    }
}
