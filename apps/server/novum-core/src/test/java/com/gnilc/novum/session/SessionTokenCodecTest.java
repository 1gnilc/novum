package com.gnilc.novum.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionTokenCodecTest {
    private final SessionTokenCodec codec = new SessionTokenCodec("customer");

    @Test
    void issuedTokensAreNamespacedUniqueAndResolveTheirUser() {
        String first = codec.issue(42L);
        String second = codec.issue(42L);

        assertThat(first).startsWith("customer.42.");
        assertThat(second).isNotEqualTo(first);
        assertThat(codec.matches(first)).isTrue();
        assertThat(codec.resolve(first)).isEqualTo(42L);
    }

    @Test
    void foreignAndMalformedTokensAreRejected() {
        assertThat(codec.matches(null)).isFalse();
        assertThat(codec.matches("sys_admin.42.value")).isFalse();
        assertThatThrownBy(() -> codec.resolve("customer.not-a-number.value"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.issue(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
