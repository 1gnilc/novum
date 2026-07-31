package com.gnilc.novum.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminSessionTokenCodecTest {
    private final AdminSessionTokenCodec codec = new AdminSessionTokenCodec();

    @Test
    void issuedTokensAreNamespacedUniqueAndResolveTheirUser() {
        String first = codec.issue(42L);
        String second = codec.issue(42L);

        assertThat(first).startsWith("sys_admin.42.");
        assertThat(second).isNotEqualTo(first);
        assertThat(codec.matches(first)).isTrue();
        assertThat(codec.resolve(first)).isEqualTo(42L);
    }

    @Test
    void invalidTokensAreRejected() {
        assertThat(codec.matches(null)).isFalse();
        assertThat(codec.matches("foreign.42.value")).isFalse();
        assertThatThrownBy(() -> codec.resolve("sys_admin.not-a-number.value"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.issue(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
