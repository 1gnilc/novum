package com.gnilc.novum.i18n;

import com.gnilc.common.i18n.I18nMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMessageBundleTest {

    @Test
    void loadsHausaAndYorubaSystemMessages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/system/messages");
        source.setDefaultEncoding("UTF-8");
        I18nMessageService messages = new I18nMessageService(source, "en-US");

        assertThat(messages.get(
                "system.image.contentLength.tooLarge", Locale.forLanguageTag("ha-NG")))
                .isEqualTo("Girman hoto ba zai wuce 3 MiB ba.");
        assertThat(messages.get(
                "system.image.contentLength.tooLarge", Locale.forLanguageTag("yo-NG")))
                .isEqualTo("Iwọn aworan ko gbọdọ kọja 3 MiB.");
    }
}
