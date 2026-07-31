package com.gnilc.novum.i18n.dao;

import com.gnilc.novum.i18n.entity.bo.I18nMessageBo;
import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.novum.support.SystemTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Transactional
class I18nMessageMapperIT {

    @Autowired
    private I18nMessageDao messages;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void mappingUsesCaseSensitiveUniqueKeyAutoFillAndPhysicalDelete() {
        I18nMessageBo lower = message("admin", "test.mapper.title", "zh-CN", "标题");
        messages.insert(lower);
        messages.insert(message("admin", "test.Mapper.title", "zh-CN", "大写标题"));

        assertThat(lower.getId()).isNotNull();
        assertThat(lower.getCreateTime()).isNotNull();
        assertThatThrownBy(() -> messages.insert(message(
                "default", "test.mapper.title", "zh-CN", "重复")))
                .isInstanceOf(DuplicateKeyException.class);

        messages.deleteById(lower.getId());

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_i18n WHERE id = ?", Integer.class, lower.getId()))
                .isZero();
    }

    private I18nMessageBo message(String category, String key, String locale, String value) {
        I18nMessageBo row = new I18nMessageBo();
        row.setCategory(category);
        row.setMessageKey(key);
        row.setLocale(locale);
        row.setI18nValue(value);
        return row;
    }
}
