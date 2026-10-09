package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.application.port.out.AudioInputRejectedException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class BoundedProcessOutput {
    private static final int BUFFER_SIZE = 8192;

    private BoundedProcessOutput() {}

    static byte[] read(InputStream input, long maxBytes) throws IOException {
        if (maxBytes <= 0 || maxBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Maximum process output must fit in a positive Java array");
        }
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new AudioInputRejectedException(
                            "AUDIO_DECODE_OUTPUT_TOO_LARGE", "Decoded audio exceeds the memory safety limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
