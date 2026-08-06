package com.gnilc.novum.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class S3ProfileConfigurationTest {
    private static final Set<String> S3_PROPERTIES = Set.of(
            "app.s3.endpoint",
            "app.s3.region",
            "app.s3.bucket",
            "app.s3.access-key",
            "app.s3.secret-key",
            "app.s3.public-base-url",
            "app.s3.presign-expiry",
            "app.s3.pending-retention",
            "app.s3.cleanup-cron",
            "app.s3.cleanup-zone",
            "app.s3.max-file-size");

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void s3ConfigurationUsesDirectProductionProfileValues() throws IOException {
        PropertySource<?> base = load("application.yml");
        PropertySource<?> prod = load("application-prod.yml");

        S3_PROPERTIES.forEach(property -> {
            assertThat(base.getProperty(property)).isNull();
            assertThat(prod.getProperty(property)).isNotNull();
            assertThat(String.valueOf(prod.getProperty(property)).contains("${"))
                    .as("%s must be a direct value", property)
                    .isFalse();
        });
    }

    private PropertySource<?> load(String resourceName) throws IOException {
        var sources = loader.load(resourceName, new ClassPathResource(resourceName));
        assertThat(sources).hasSize(1);
        return sources.get(0);
    }
}
