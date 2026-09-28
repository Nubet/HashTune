package com.norbertfila.hashtune.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.norbertfila.hashtune.adapter.out.storage.StorageException;
import com.norbertfila.hashtune.application.service.ApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = request("/api/v1/library/tracks");

    @Test
    void returnsApplicationErrorContract() {
        var response = handler.handle(
                new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_AUDIO", "Audio file is invalid"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).satisfies(problem -> {
            assertThat(problem.code()).isEqualTo("INVALID_AUDIO");
            assertThat(problem.detail()).isEqualTo("Audio file is invalid");
            assertThat(problem.instance()).isEqualTo("/api/v1/library/tracks");
        });
    }

    @Test
    void mapsStorageFailureToRetryableError() {
        var response = handler.handleStorage(new StorageException("MinIO failed", new RuntimeException()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).satisfies(problem -> {
            assertThat(problem.code()).isEqualTo("STORAGE_UNAVAILABLE");
            assertThat(problem.detail()).contains("temporarily unavailable");
        });
    }

    @Test
    void mapsMissingMultipartFileToFieldError() {
        var response = handler.handleMissingPart(new MissingServletRequestPartException("file"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .satisfies(problem -> assertThat(problem.errors()).containsEntry("file", "File is required."));
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
