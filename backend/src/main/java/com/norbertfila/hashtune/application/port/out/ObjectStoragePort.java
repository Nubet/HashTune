package com.norbertfila.hashtune.application.port.out;

import java.io.InputStream;

public interface ObjectStoragePort {
    String put(String bucket, String objectKey, InputStream content, long size, String contentType);

    InputStream get(String bucket, String objectKey);

    void delete(String bucket, String objectKey);
}
