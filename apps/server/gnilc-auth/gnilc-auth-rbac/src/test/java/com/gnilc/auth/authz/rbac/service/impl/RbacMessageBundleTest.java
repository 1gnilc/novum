package com.gnilc.auth.authz.rbac.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RbacMessageBundleTest extends RbacMessageTestSupport {

    @Test
    void loadsHausaAndYorubaRbacMessagesWithArguments() {
        assertThat(messages().get(
                "rbac.role.code.tooLong", Locale.forLanguageTag("ha-NG"), 255))
                .isEqualTo("Lambar rawar aiki ba za ta wuce haruffa 255 ba.");
        assertThat(messages().get(
                "rbac.role.code.tooLong", Locale.forLanguageTag("yo-NG"), 255))
                .isEqualTo("Koodu ipa ko gbọdọ kọja awọn ohun kikọ 255.");
    }
}
