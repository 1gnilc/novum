package com.gnilc.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {
    @Test
    void calculatesTotalPages() {
        PageResult<String> result = new PageResult<>(List.of("a", "b"), 11, 5, 2);

        assertThat(result.getTotalPage()).isEqualTo(3);
        assertThat(result.getCurrentPage()).isEqualTo(2);
    }

    @Test
    void serializesPaginationMetadataAsNumbersWhenLongsAreGloballyStrings() throws Exception {
        SimpleModule longAsString = new SimpleModule();
        longAsString.addSerializer(Long.class, ToStringSerializer.instance);
        longAsString.addSerializer(Long.TYPE, ToStringSerializer.instance);
        ObjectMapper mapper = new ObjectMapper().registerModule(longAsString);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(
                new PageResult<>(List.of(new Item(9L)), 11L, 5L, 2L)));

        assertThat(json.get("totalPage").isIntegralNumber()).isTrue();
        assertThat(json.get("totalCount").isIntegralNumber()).isTrue();
        assertThat(json.get("pageSize").isIntegralNumber()).isTrue();
        assertThat(json.get("currentPage").isIntegralNumber()).isTrue();
        assertThat(json.get("list").get(0).get("id").isTextual()).isTrue();
    }

    @Data
    private static final class Item {
        private final Long id;
    }
}
