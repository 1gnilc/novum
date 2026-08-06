package com.gnilc.novum.s3;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3PropertiesTest {

    @Test
    void rejectsAConfiguredMaximumAboveTheThreeMebibyteLimit() {
        S3Properties properties = validProperties();
        properties.setMaxFileSize(S3Properties.MAX_IMAGE_FILE_SIZE + 1);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("S3 maximum file size must not exceed 3 MiB.");
    }

    private static S3Properties validProperties() {
        S3Properties properties = new S3Properties();
        properties.setEndpoint(URI.create("https://s3.example.test"));
        properties.setBucket("images");
        properties.setAccessKey("access-key");
        properties.setSecretKey("secret-key");
        properties.setPublicBaseUrl("https://images.example.test");
        return properties;
    }
}
