package com.gnilc.novum.s3;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

final class DefaultS3Service implements S3Service {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;
    private final Clock clock;

    DefaultS3Service(
            S3Client client,
            S3Presigner presigner,
            S3Properties properties,
            Clock clock) {
        this.client = client;
        this.presigner = presigner;
        this.bucket = properties.getBucket();
        this.clock = clock;
    }

    @Override
    public S3UploadSignature presignUpload(S3UploadRequest request) {
        PutObjectRequest putObject = PutObjectRequest.builder()
                .bucket(bucket)
                .key(request.getObjectKey())
                .contentType(request.getContentType())
                .contentLength(request.getContentLength())
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(request.getExpiry())
                        .putObjectRequest(putObject)
                        .build());
        Map<String, String> headers = new LinkedHashMap<>();
        presigned.signedHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("content-length") && !name.equalsIgnoreCase("host")
                    && !values.isEmpty()) {
                headers.put(normalizeBrowserHeaderName(name), values.get(0));
            }
        });
        if (headers.keySet().stream().noneMatch(name -> name.equalsIgnoreCase("content-type"))) {
            headers.put("Content-Type", request.getContentType());
        }
        return new S3UploadSignature(
                URI.create(presigned.url().toString()),
                Map.copyOf(headers),
                clock.instant().plus(request.getExpiry()));
    }

    @Override
    public S3ObjectMetadata getObjectMetadata(String objectKey) {
        try {
            HeadObjectResponse response = client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            return new S3ObjectMetadata(response.contentType(), response.contentLength());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return null;
            }
            throw exception;
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());
    }

    private static String normalizeBrowserHeaderName(String name) {
        return name.equalsIgnoreCase("content-type") ? "Content-Type" : name;
    }
}
