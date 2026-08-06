package com.gnilc.novum.image.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.image.dao.ImageDao;
import com.gnilc.novum.image.entity.bo.ImageBo;
import com.gnilc.novum.image.entity.enums.ImageStatus;
import com.gnilc.novum.image.entity.vo.ImagePresignVo;
import com.gnilc.novum.image.entity.vo.ImageVo;
import com.gnilc.novum.s3.S3ObjectMetadata;
import com.gnilc.novum.s3.S3Properties;
import com.gnilc.novum.s3.S3Service;
import com.gnilc.novum.s3.S3UploadRequest;
import com.gnilc.novum.s3.S3UploadSignature;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-08-05T04:30:00Z");
    private static final long MAX_SIZE = 3_145_728L;

    @Mock
    private ImageDao imageDao;
    @Mock
    private S3Service s3Service;

    private ImageServiceImpl images;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(ImageBo.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "image-service-test"),
                    ImageBo.class);
        }
        S3Properties properties = new S3Properties();
        properties.setPublicBaseUrl("https://images.example.test/");
        properties.setPresignExpiry(Duration.ofMinutes(10));
        properties.setPendingRetention(Duration.ofHours(24));
        properties.setMaxFileSize(MAX_SIZE);
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/system/messages");
        source.setDefaultEncoding("UTF-8");
        I18nMessageService messages = new I18nMessageService(source, "en-US");
        images = spy(new ImageServiceImpl(
                s3Service,
                properties,
                messages,
                Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test
    void presignCreatesPendingImageAndReturnsPutContract() {
        URI uploadUrl = URI.create("https://upload.example.test/signed");
        when(s3Service.presignUpload(any())).thenReturn(new S3UploadSignature(
                uploadUrl,
                Map.of("Content-Type", "image/webp"),
                NOW.plus(Duration.ofMinutes(10))));
        doReturn(true).when(images).save(any(ImageBo.class));

        ImagePresignVo result = images.presign("image/webp", MAX_SIZE);

        assertThat(result.getObjectKey())
                .matches("images/2026/08/05/[0-9a-f-]{36}\\.webp");
        assertThat(result.getUploadUrl()).isEqualTo(uploadUrl.toString());
        assertThat(result.getMethod()).isEqualTo("PUT");
        assertThat(result.getHeaders()).containsEntry("Content-Type", "image/webp");
        assertThat(result.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));

        ArgumentCaptor<ImageBo> image = ArgumentCaptor.forClass(ImageBo.class);
        verify(images).save(image.capture());
        assertThat(image.getValue().getStatus()).isEqualTo(ImageStatus.PENDING);
        assertThat(image.getValue().getContentLength()).isEqualTo(MAX_SIZE);
        assertThat(image.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));

        ArgumentCaptor<S3UploadRequest> request = ArgumentCaptor.forClass(S3UploadRequest.class);
        verify(s3Service).presignUpload(request.capture());
        assertThat(request.getValue().getContentLength()).isEqualTo(MAX_SIZE);
        assertThat(request.getValue().getContentType()).isEqualTo("image/webp");
    }

    @Test
    void presignRejectsUnsupportedOrOversizedFilesBeforeStorage() {
        assertThatThrownBy(() -> images.presign("image/svg+xml", 1024L))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Only JPEG, PNG, and WebP images are supported.");
        assertThatThrownBy(() -> images.presign("image/png", MAX_SIZE + 1))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Image size must not exceed 3 MiB.");
        verify(s3Service, never()).presignUpload(any());
    }

    @Test
    void finalizeVerifiesMetadataAndReturnsReadyImageIdempotently() {
        ImageBo pending = pending("images/2026/08/05/photo.png", "image/png", 2048L);
        when(s3Service.getObjectMetadata(pending.getObjectKey()))
                .thenReturn(new S3ObjectMetadata("image/png", 2048L));
        doReturn(pending).when(images).getByObjectKey(pending.getObjectKey());
        doReturn(true).when(images).updateById(pending);

        ImageVo first = images.finalize(pending.getObjectKey());

        assertThat(pending.getStatus()).isEqualTo(ImageStatus.READY);
        assertThat(pending.getExpiresAt()).isNull();
        assertThat(first.getUrl()).isEqualTo(
                "https://images.example.test/images/2026/08/05/photo.png");

        doReturn(pending).when(images).getByObjectKey(pending.getObjectKey());
        ImageVo second = images.finalize(pending.getObjectKey());
        assertThat(second).isEqualTo(first);
        verify(s3Service).getObjectMetadata(pending.getObjectKey());
    }

    @Test
    void finalizeDeletesObjectWhenStoredMetadataExceedsLimit() {
        ImageBo pending = pending("images/2026/08/05/photo.jpg", "image/jpeg", 1024L);
        doReturn(pending).when(images).getByObjectKey(pending.getObjectKey());
        when(s3Service.getObjectMetadata(pending.getObjectKey()))
                .thenReturn(new S3ObjectMetadata("image/jpeg", MAX_SIZE + 1));

        assertThatThrownBy(() -> images.finalize(pending.getObjectKey()))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Stored image metadata does not match the upload request.");
        verify(s3Service).deleteObject(pending.getObjectKey());
        verify(images, never()).updateById(pending);
    }

    private static ImageBo pending(String objectKey, String contentType, long contentLength) {
        ImageBo image = new ImageBo();
        image.setId(42L);
        image.setCreateTime(NOW);
        image.setObjectKey(objectKey);
        image.setContentType(contentType);
        image.setContentLength(contentLength);
        image.setStatus(ImageStatus.PENDING);
        image.setExpiresAt(NOW.plus(Duration.ofHours(24)));
        return image;
    }
}
