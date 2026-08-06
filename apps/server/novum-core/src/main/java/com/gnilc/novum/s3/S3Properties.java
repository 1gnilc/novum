package com.gnilc.novum.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {
    public static final long MAX_IMAGE_FILE_SIZE = 3_145_728L;

    private URI endpoint;
    private String region = "auto";
    private String bucket;
    private String accessKey;
    private String secretKey;
    private String publicBaseUrl;
    private Duration presignExpiry = Duration.ofMinutes(10);
    private Duration pendingRetention = Duration.ofHours(24);
    private String cleanupCron = "0 0 3 * * *";
    private String cleanupZone = "UTC";
    private long maxFileSize = MAX_IMAGE_FILE_SIZE;

    public void validate() {
        require(endpoint != null, "S3 endpoint must be configured.");
        requireNotBlank(region, "S3 region must be configured.");
        requireNotBlank(bucket, "S3 bucket must be configured.");
        requireNotBlank(accessKey, "S3 access key must be configured.");
        requireNotBlank(secretKey, "S3 secret key must be configured.");
        requireNotBlank(publicBaseUrl, "S3 public base URL must be configured.");
        require(presignExpiry != null && !presignExpiry.isNegative() && !presignExpiry.isZero(),
                "S3 presign expiry must be positive.");
        require(pendingRetention != null && !pendingRetention.isNegative() && !pendingRetention.isZero(),
                "S3 pending retention must be positive.");
        requireNotBlank(cleanupCron, "S3 cleanup cron must be configured.");
        requireNotBlank(cleanupZone, "S3 cleanup zone must be configured.");
        require(maxFileSize > 0, "S3 maximum file size must be positive.");
        require(maxFileSize <= MAX_IMAGE_FILE_SIZE,
                "S3 maximum file size must not exceed 3 MiB.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireNotBlank(String value, String message) {
        require(value != null && !value.isBlank(), message);
    }

}
