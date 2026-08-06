package com.gnilc.novum.s3;

/**
 * S3 public URL helpers.
 */
public final class S3UrlUtils {

    private S3UrlUtils() {
    }

    public static String getUrl(String publicBaseUrl, String objectKey) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalArgumentException("S3 public base URL must not be blank.");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("S3 object key must not be blank.");
        }
        return stripTrailingSlashes(publicBaseUrl.trim()) + "/" + stripLeadingSlashes(objectKey.trim());
    }

    private static String stripLeadingSlashes(String value) {
        int index = 0;
        while (index < value.length() && value.charAt(index) == '/') {
            index++;
        }
        return value.substring(index);
    }

    private static String stripTrailingSlashes(String value) {
        int index = value.length();
        while (index > 0 && value.charAt(index - 1) == '/') {
            index--;
        }
        return value.substring(0, index);
    }
}
