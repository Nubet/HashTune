package com.norbertfila.hashtune.service.validation;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BoundedInputStream extends FilterInputStream {
    private long remaining;

    public BoundedInputStream(InputStream input, long maxBytes) {
        super(input);
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("Maximum stream size must be positive");
        }
        remaining = maxBytes;
    }

    @Override
    public int read() throws IOException {
        if (remaining == 0) {
            ensureEndOfStream();
            return -1;
        }
        int value = super.read();
        if (value != -1) {
            remaining--;
        }
        return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        if (remaining == 0) {
            ensureEndOfStream();
            return -1;
        }
        int read = super.read(bytes, offset, (int) Math.min(length, remaining));
        if (read > 0) {
            remaining -= read;
        }
        return read;
    }

    private void ensureEndOfStream() throws IOException {
        if (super.read() != -1) {
            throw new IOException("Stream exceeds the configured size limit");
        }
    }
}
