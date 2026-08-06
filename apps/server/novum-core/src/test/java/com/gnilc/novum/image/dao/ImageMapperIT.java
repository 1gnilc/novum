package com.gnilc.novum.image.dao;

import com.gnilc.novum.image.entity.bo.ImageBo;
import com.gnilc.novum.image.entity.enums.ImageStatus;
import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.novum.support.SystemTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Transactional
class ImageMapperIT {
    @Autowired
    private ImageDao images;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void imageMappingSupportsLogicalAndPhysicalDelete() {
        ImageBo logical = image("images/2026/08/05/11111111-1111-1111-1111-111111111111.png");
        images.insert(logical);

        assertThat(logical.getId()).isNotNull();
        assertThat(logical.getCreateTime()).isNotNull();
        assertThat(images.selectById(logical.getId()).getStatus()).isEqualTo(ImageStatus.PENDING);

        images.deleteById(logical.getId());

        assertThat(images.selectById(logical.getId())).isNull();
        assertThat(jdbc.queryForObject(
                "select del from sys_image where id = ?", Integer.class, logical.getId()))
                .isEqualTo(1);

        ImageBo physical = image("images/2026/08/05/22222222-2222-2222-2222-222222222222.webp");
        images.insert(physical);

        assertThat(images.hardDeleteById(physical.getId())).isEqualTo(1);
        assertThat(images.hardDeleteById(Long.MAX_VALUE)).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from sys_image where id = ?", Integer.class, physical.getId()))
                .isZero();
    }

    private static ImageBo image(String objectKey) {
        ImageBo image = new ImageBo();
        image.setObjectKey(objectKey);
        image.setContentType(objectKey.endsWith(".webp") ? "image/webp" : "image/png");
        image.setContentLength(2048L);
        image.setStatus(ImageStatus.PENDING);
        image.setExpiresAt(Instant.parse("2026-08-06T04:30:00Z"));
        return image;
    }
}
