package com.gnilc.novum.image.support;

import com.gnilc.novum.s3.S3ObjectMetadata;
import com.gnilc.novum.s3.S3Service;
import com.gnilc.novum.s3.S3UploadRequest;
import com.gnilc.novum.s3.S3UploadSignature;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration(proxyBeanMethods = false)
public class ImageApiTestConfiguration {

    @Bean
    @Primary
    TestS3Service testS3Service() {
        return new TestS3Service();
    }

    public static final class TestS3Service implements S3Service {
        private final Map<String, S3UploadRequest> requestedUploads = new ConcurrentHashMap<>();
        private final Map<String, S3ObjectMetadata> uploadedObjects = new ConcurrentHashMap<>();
        private final Set<String> deletedObjects = ConcurrentHashMap.newKeySet();

        @Override
        public S3UploadSignature presignUpload(S3UploadRequest request) {
            requestedUploads.put(request.getObjectKey(), request);
            return new S3UploadSignature(
                    URI.create("https://upload.example.test/" + request.getObjectKey() + "?signature=test"),
                    Map.of("Content-Type", request.getContentType()),
                    Instant.now().plus(request.getExpiry()));
        }

        @Override
        public S3ObjectMetadata getObjectMetadata(String objectKey) {
            return uploadedObjects.get(objectKey);
        }

        @Override
        public void deleteObject(String objectKey) {
            uploadedObjects.remove(objectKey);
            deletedObjects.add(objectKey);
        }

        public void markUploaded(String objectKey) {
            S3UploadRequest request = requestedUploads.get(objectKey);
            if (request == null) {
                throw new IllegalArgumentException("Upload was not presigned: " + objectKey);
            }
            uploadedObjects.put(objectKey,
                    new S3ObjectMetadata(request.getContentType(), request.getContentLength()));
        }

        public boolean contains(String objectKey) {
            return uploadedObjects.containsKey(objectKey);
        }

        public boolean wasDeleted(String objectKey) {
            return deletedObjects.contains(objectKey);
        }

        public void clear() {
            requestedUploads.clear();
            uploadedObjects.clear();
            deletedObjects.clear();
        }
    }
}
