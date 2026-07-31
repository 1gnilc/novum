package com.gnilc.novum.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class I18nMessageBundleTest {

    private static final List<String> BASENAMES = List.of(
            "i18n/common/messages",
            "i18n/rbac/messages",
            "i18n/system/messages");

    @Test
    void everyBackendBundleHasMatchingLocalesAndUniqueOwnership() throws Exception {
        Map<String, String> owners = new HashMap<>();

        for (String basename : BASENAMES) {
            Set<String> defaults = keys(basename + ".properties");
            assertThat(keys(basename + "_zh_CN.properties")).isEqualTo(defaults);
            assertThat(keys(basename + "_en_US.properties")).isEqualTo(defaults);
            for (String key : defaults) {
                assertThat(owners.putIfAbsent(key, basename))
                        .as("message key %s must have one owning module", key)
                        .isNull();
            }
        }
    }

    private Set<String> keys(String path) throws Exception {
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties.stringPropertyNames();
    }
}
