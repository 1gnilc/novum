package com.gnilc.novum.auth;

import com.gnilc.common.i18n.SupportedLocale;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

final class AuthLocaleTestSupport {
    private AuthLocaleTestSupport() {
    }

    static LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SupportedLocale.locales());
        resolver.setDefaultLocale(Locale.US);
        return resolver;
    }
}
