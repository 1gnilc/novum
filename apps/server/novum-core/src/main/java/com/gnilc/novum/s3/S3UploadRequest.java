package com.gnilc.novum.s3;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

@Data
@AllArgsConstructor
public class S3UploadRequest {
    private final String objectKey;
    private final String contentType;
    private final long contentLength;
    private final Duration expiry;
}
