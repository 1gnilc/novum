package com.gnilc.novum.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultS3ServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-05T04:30:00Z");

    @Mock
    private S3Client client;
    @Mock
    private S3Presigner presigner;

    private DefaultS3Service service;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties();
        properties.setBucket("images");
        service = new DefaultS3Service(
                client, presigner, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void presignUploadReturnsOneBrowserSettableContentTypeHeader() {
        Map<String, List<String>> signedHeaders = new LinkedHashMap<>();
        signedHeaders.put("content-length", List.of("2048"));
        signedHeaders.put("content-type", List.of("image/png"));
        signedHeaders.put("host", List.of("upload.example.test"));
        PresignedPutObjectRequest presigned = PresignedPutObjectRequest.builder()
                .expiration(NOW.plus(Duration.ofMinutes(10)))
                .isBrowserExecutable(false)
                .signedHeaders(signedHeaders)
                .httpRequest(SdkHttpRequest.builder()
                        .uri(URI.create("https://upload.example.test/signed"))
                        .method(SdkHttpMethod.PUT)
                        .build())
                .build();
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        S3UploadSignature signature = service.presignUpload(new S3UploadRequest(
                "images/2026/08/05/79f91166-e852-4a9d-a419-dabfb427cb8c.png",
                "image/png",
                2048,
                Duration.ofMinutes(10)));

        assertThat(signature.getHeaders()).containsExactly(Map.entry("Content-Type", "image/png"));
        assertThat(signature.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void getObjectMetadataReturnsNullWhenTheObjectDoesNotExist() {
        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(
                S3Exception.builder().statusCode(404).message("Not Found").build());

        assertThat(service.getObjectMetadata("images/missing.png")).isNull();
    }
}
