package com.gnilc.common.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class I18nMessageServiceTest {

    private I18nMessageService messages;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("greeting", Locale.SIMPLIFIED_CHINESE, "你好，{0}");
        messageSource.addMessage("greeting", Locale.US, "Hello, {0}");
        messages = new I18nMessageService(messageSource, "zh-CN");
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void resolvesCurrentAndExplicitSupportedLocalesWithArguments() {
        LocaleContextHolder.setLocale(Locale.US);

        assertThat(messages.get("greeting", "Alice")).isEqualTo("Hello, Alice");
        assertThat(messages.get("greeting", Locale.SIMPLIFIED_CHINESE, "小明"))
                .isEqualTo("你好，小明");
    }

    @Test
    void fallsBackToConfiguredLocaleAndReturnsMissingCode() {
        LocaleContextHolder.setLocale(Locale.FRANCE);

        assertThat(messages.get("greeting", "Alice")).isEqualTo("你好，Alice");
        assertThat(messages.get("missing.key")).isEqualTo("missing.key");
        assertThat(messages.getOrDefault("missing.key", "Fallback"))
                .isEqualTo("Fallback");
    }

    @Test
    void fallsBackWhenARequestedLocaleIsOnlyAUnsupportedVariant() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("en-US-POSIX"));

        assertThat(messages.get("greeting", "Alice")).isEqualTo("你好，Alice");
    }

    @Test
    void invalidDefaultLocaleFallsBackToEnglish() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("greeting", Locale.SIMPLIFIED_CHINESE, "你好");
        messageSource.addMessage("greeting", Locale.US, "Hello");
        I18nMessageService messagesWithInvalidDefault =
                new I18nMessageService(messageSource, "unsupported");

        assertThat(messagesWithInvalidDefault.get("greeting", Locale.FRANCE))
                .isEqualTo("Hello");
    }

    @Test
    void rejectsBlankMessageCode() {
        assertThatThrownBy(() -> messages.get(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Internationalization code must not be blank.");
    }

    @Test
    void exposesAllSupportedStaticLocales() {
        assertThat(SupportedLocale.codes())
                .containsExactly("zh-CN", "en-US", "ha-NG", "yo-NG");
    }

    @Test
    void loadsHausaAndYorubaCommonMessages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/common/messages");
        source.setDefaultEncoding("UTF-8");
        I18nMessageService bundleMessages = new I18nMessageService(source, "en-US");

        assertThat(bundleMessages.get(
                "validation.argument.invalid", Locale.forLanguageTag("ha-NG")))
                .isEqualTo("Buƙatun ya ƙunshi filayen da ba daidai ba.");
        assertThat(bundleMessages.get(
                "validation.argument.invalid", Locale.forLanguageTag("yo-NG")))
                .isEqualTo("Ibeere naa ni awọn aaye ti ko wulo.");
    }
}
