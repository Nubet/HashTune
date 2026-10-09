package com.norbertfila.hashtune.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BoundedInputStreamTest {
    @Test
    void streamsContentWithinLimit() throws Exception {
        try (BoundedInputStream input = new BoundedInputStream(
                new ByteArrayInputStream("cover".getBytes(StandardCharsets.UTF_8)), 5)) {
            assertThat(input.readAllBytes()).isEqualTo("cover".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void rejectsContentBeyondLimit() throws Exception {
        try (BoundedInputStream input = new BoundedInputStream(
                new ByteArrayInputStream("covers".getBytes(StandardCharsets.UTF_8)), 5)) {
            byte[] buffer = new byte[5];
            assertThat(input.read(buffer)).isEqualTo(5);
            assertThatThrownBy(input::read).isInstanceOf(java.io.IOException.class);
        }
    }
}
