package com.gnilc.novum.s3;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3UrlUtilsTest {

    @Test
    void joinsPublicBaseUrlAndObjectKeyWithOneSeparator() {
        assertThat(S3UrlUtils.getUrl("https://images.example.test", "images/2026/08/photo.webp"))
                .isEqualTo("https://images.example.test/images/2026/08/photo.webp");
        assertThat(S3UrlUtils.getUrl("https://images.example.test/", "/images/2026/08/photo.webp"))
                .isEqualTo("https://images.example.test/images/2026/08/photo.webp");
    }

    @Test
    void rejectsMissingUrlParts() {
        assertThatThrownBy(() -> S3UrlUtils.getUrl(" ", "images/photo.png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> S3UrlUtils.getUrl("https://images.example.test", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
