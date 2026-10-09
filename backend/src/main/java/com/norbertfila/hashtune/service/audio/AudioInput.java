package com.norbertfila.hashtune.service.audio;

import java.io.IOException;
import java.io.InputStream;

public record AudioInput(String originalFilename, String contentType, long size, InputStreamSource source) {
    public InputStream open() throws IOException {
        return source.open();
    }

    @FunctionalInterface
    public interface InputStreamSource {
        InputStream open() throws IOException;
    }
}
