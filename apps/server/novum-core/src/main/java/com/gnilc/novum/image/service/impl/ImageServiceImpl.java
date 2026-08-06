package com.gnilc.novum.image.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.PageParams;
import com.gnilc.common.utils.PageResult;
import com.gnilc.novum.image.dao.ImageDao;
import com.gnilc.novum.image.entity.bo.ImageBo;
import com.gnilc.novum.image.entity.enums.ImageStatus;
import com.gnilc.novum.image.entity.vo.ImagePresignVo;
import com.gnilc.novum.image.entity.vo.ImageVo;
import com.gnilc.novum.image.service.ImageService;
import com.gnilc.novum.s3.S3ObjectMetadata;
import com.gnilc.novum.s3.S3Properties;
import com.gnilc.novum.s3.S3Service;
import com.gnilc.novum.s3.S3UploadRequest;
import com.gnilc.novum.s3.S3UploadSignature;
import com.gnilc.novum.s3.S3UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageServiceImpl extends ServiceImpl<ImageDao, ImageBo> implements ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageServiceImpl.class);
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final S3Service s3Service;
    private final S3Properties properties;
    private final I18nMessageService messages;
    private final Clock clock;

    public ImageServiceImpl(
            S3Service s3Service,
            S3Properties properties,
            I18nMessageService messages,
            Clock clock) {
        this.s3Service = s3Service;
        this.properties = properties;
        this.messages = messages;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ImagePresignVo presign(String contentType, Long contentLength) {
        String normalizedContentType = StringUtils.trimToEmpty(contentType).toLowerCase(Locale.ROOT);
        Preconditions.checkArgument(EXTENSIONS.containsKey(normalizedContentType),
                messages.get("system.image.contentType.unsupported"));
        Preconditions.checkArgument(contentLength != null && contentLength > 0,
                messages.get("system.image.contentLength.invalid"));
        Preconditions.checkArgument(contentLength <= properties.getMaxFileSize(),
                messages.get("system.image.contentLength.tooLarge"));

        Instant now = clock.instant();
        String objectKey = createObjectKey(normalizedContentType, now);
        S3UploadSignature signature = s3Service.presignUpload(new S3UploadRequest(
                objectKey,
                normalizedContentType,
                contentLength,
                properties.getPresignExpiry()));

        ImageBo image = new ImageBo();
        image.setObjectKey(objectKey);
        image.setContentType(normalizedContentType);
        image.setContentLength(contentLength);
        image.setStatus(ImageStatus.PENDING);
        image.setExpiresAt(now.plus(properties.getPendingRetention()));
        save(image);

        ImagePresignVo result = new ImagePresignVo();
        result.setObjectKey(objectKey);
        result.setUploadUrl(signature.getUploadUrl().toString());
        result.setMethod("PUT");
        result.setHeaders(signature.getHeaders());
        result.setExpiresAt(signature.getExpiresAt());
        return result;
    }

    @Override
    @Transactional
    public ImageVo finalize(String objectKey) {
        String normalizedObjectKey = StringUtils.trimToNull(objectKey);
        Preconditions.checkArgument(normalizedObjectKey != null, messages.get("system.image.objectKey.required"));
        ImageBo image = getByObjectKey(normalizedObjectKey);
        Preconditions.checkCondition(image != null, messages.get("system.image.notFound"));
        if (image.getStatus() == ImageStatus.READY) {
            return toVo(image);
        }
        Preconditions.checkCondition(image.getExpiresAt() == null || clock.instant().isBefore(image.getExpiresAt()),
                messages.get("system.image.upload.expired"));

        S3ObjectMetadata metadata = s3Service.getObjectMetadata(normalizedObjectKey);
        boolean matches = metadata != null
                && image.getContentType().equalsIgnoreCase(StringUtils.trimToEmpty(metadata.getContentType()))
                && image.getContentLength() == metadata.getContentLength()
                && metadata.getContentLength() <= properties.getMaxFileSize();
        if (!matches) {
            s3Service.deleteObject(normalizedObjectKey);
            throw new IllegalConditionException(messages.get("system.image.metadata.mismatch"));
        }

        image.setStatus(ImageStatus.READY);
        image.setExpiresAt(null);
        updateById(image);
        return toVo(image);
    }

    @Override
    public PageResult<ImageVo> getImagePage(PageParams params) {
        IPage<ImageBo> page = lambdaQuery()
                .eq(ImageBo::getStatus, ImageStatus.READY)
                .orderByDesc(ImageBo::getId)
                .page(params.getPage());
        return PageResult.of(page, page.getRecords().stream().map(this::toVo).toList());
    }

    @Override
    @Transactional
    public void removeImage(Long id) {
        Preconditions.checkArgument(id != null, messages.get("system.image.selection.required"));
        ImageBo image = getById(id);
        Preconditions.checkCondition(image != null, messages.get("system.image.notFound"));
        s3Service.deleteObject(image.getObjectKey());
        removeById(id);
    }

    @Override
    public String getUrl(String objectKey) {
        return S3UrlUtils.getUrl(properties.getPublicBaseUrl(), objectKey);
    }

    ImageBo getByObjectKey(String objectKey) {
        return lambdaQuery().eq(ImageBo::getObjectKey, objectKey).one();
    }

    @Scheduled(
            cron = "${app.s3.cleanup-cron:0 0 3 * * *}",
            zone = "${app.s3.cleanup-zone:UTC}")
    public void cleanupExpiredPendingImages() {
        Instant now = clock.instant();
        List<ImageBo> expired = lambdaQuery()
                .eq(ImageBo::getStatus, ImageStatus.PENDING)
                .le(ImageBo::getExpiresAt, now)
                .list();
        for (ImageBo image : expired) {
            try {
                s3Service.deleteObject(image.getObjectKey());
                baseMapper.hardDeleteById(image.getId());
            } catch (RuntimeException exception) {
                log.warn("Failed to clean expired image objectKey={}", image.getObjectKey(), exception);
            }
        }
    }

    private String createObjectKey(String contentType, Instant now) {
        LocalDate date = now.atZone(ZoneOffset.UTC).toLocalDate();
        return "images/" + date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + UUID.randomUUID() + "." + EXTENSIONS.get(contentType);
    }

    private ImageVo toVo(ImageBo image) {
        ImageVo result = new ImageVo();
        result.setId(image.getId());
        result.setObjectKey(image.getObjectKey());
        result.setUrl(getUrl(image.getObjectKey()));
        result.setContentType(image.getContentType());
        result.setContentLength(image.getContentLength());
        result.setStatus(image.getStatus());
        result.setCreateTime(image.getCreateTime());
        return result;
    }
}
