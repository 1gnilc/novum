package com.gnilc.novum.s3;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class S3ObjectMetadata {
    private final String contentType;
    private final long contentLength;
}
