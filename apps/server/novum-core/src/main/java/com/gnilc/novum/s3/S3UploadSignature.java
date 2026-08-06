package com.gnilc.novum.s3;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
public class S3UploadSignature {
    private final URI uploadUrl;
    private final Map<String, String> headers;
    private final Instant expiresAt;
}
