package com.gnilc.novum.s3;

public interface S3Service {
    S3UploadSignature presignUpload(S3UploadRequest request);

    S3ObjectMetadata getObjectMetadata(String objectKey);

    void deleteObject(String objectKey);
}
