package com.norbertfila.hashtune.service.storage;

import com.norbertfila.hashtune.exceptions.storage.StorageException;
import com.norbertfila.hashtune.service.storage.ObjectStoragePort;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioStorageAdapter implements ObjectStoragePort {
    private final MinioClient client;

    @Override
    public String put(String bucket, String objectKey, InputStream content, long size, String contentType) {
        try {
            ensureBucket(bucket);
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(content, size, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
            return objectKey;
        } catch (Exception exception) {
            throw new StorageException("Could not store object", exception);
        }
    }

    @Override
    public InputStream get(String bucket, String objectKey) {
        try {
            return client.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) {
            throw new StorageException("Could not read object", exception);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            client.removeObject(io.minio.RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new StorageException("Could not delete object", exception);
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
