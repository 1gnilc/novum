package com.gnilc.novum.admin.dao;

import com.gnilc.novum.admin.entity.bo.AdminBo;
import com.gnilc.novum.support.SystemTestApplication;
import com.gnilc.novum.support.SystemContainerContextInitializer;
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
class AdminMapperIT {
    @Autowired private AdminDao admins;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void adminMappingUsesUniqueIndexesAutoFillAndLogicalDelete() {
        AdminBo admin = admin("mapper-user", 9001L);
        admins.insert(admin);

        assertThat(admin.getId()).isNotNull();
        assertThat(admin.getCreateTime()).isNotNull();
        assertThat(admins.selectById(admin.getId()).getHomePath()).isEqualTo("/workspace");
        assertThatThrownBy(() -> admins.insert(admin("mapper-user", 9002L)))
                .isInstanceOf(DuplicateKeyException.class);

        admins.deleteById(admin.getId());

        assertThat(admins.selectById(admin.getId())).isNull();
        assertThat(jdbc.queryForObject(
                "select del from sys_admin where id = ?", Integer.class, admin.getId())).isEqualTo(1);
    }

    private AdminBo admin(String username, Long userId) {
        AdminBo admin = new AdminBo();
        admin.setUserId(userId);
        admin.setUsername(username);
        admin.setPassword("hash");
        admin.setNickname(username);
        admin.setHomePath("/workspace");
        admin.setStatus(true);
        return admin;
    }
}
