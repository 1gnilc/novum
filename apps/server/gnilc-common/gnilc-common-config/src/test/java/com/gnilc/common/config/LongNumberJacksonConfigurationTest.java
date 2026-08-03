package com.gnilc.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LongNumberJacksonConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    LongNumberJacksonConfiguration.class));

    @Test
    void serializesLongNumbersAsStrings() {
        contextRunner.run(context -> {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            assertThat(objectMapper.writeValueAsString(
                    new LongNumbers(
                            9007199254740991L,
                            9007199254740992L,
                            Long.MAX_VALUE,
                            new BigInteger("9223372036854775808"))))
                    .isEqualTo("{\"maximumSafeInteger\":\"9007199254740991\","
                            + "\"firstUnsafeInteger\":\"9007199254740992\","
                            + "\"maximumLong\":\"9223372036854775807\","
                            + "\"beyondLong\":\"9223372036854775808\"}");
        });
    }

    @Test
    void coexistsWithOtherJacksonCustomizers() {
        contextRunner
                .withBean("otherJacksonCustomizer", Jackson2ObjectMapperBuilderCustomizer.class,
                        () -> builder -> { })
                .run(context -> assertThat(context)
                        .getBeans(Jackson2ObjectMapperBuilderCustomizer.class)
                        .containsKeys("longNumberJacksonCustomizer", "otherJacksonCustomizer"));
    }

    @Test
    void backsOffForAReplacementWithTheSameName() {
        Jackson2ObjectMapperBuilderCustomizer replacement = builder ->
                builder.serializerByType(Long.class, NumberSerializer.instance);

        contextRunner
                .withBean("longNumberJacksonCustomizer", Jackson2ObjectMapperBuilderCustomizer.class,
                        () -> replacement)
                .run(context -> {
                    assertThat(context)
                            .getBeans(Jackson2ObjectMapperBuilderCustomizer.class)
                            .containsEntry("longNumberJacksonCustomizer", replacement);
                    assertThat(context.getBeansOfType(Jackson2ObjectMapperBuilderCustomizer.class))
                            .hasSize(2);
                });
    }

    @Data
    private static final class LongNumbers {
        private final Long maximumSafeInteger;
        private final long firstUnsafeInteger;
        private final long maximumLong;
        private final BigInteger beyondLong;
    }
}
