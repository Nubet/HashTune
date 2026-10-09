package com.norbertfila.hashtune.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.norbertfila.hashtune.exceptions.application.ApplicationException;
import com.norbertfila.hashtune.exceptions.security.AuthenticationRequiredException;
import com.norbertfila.hashtune.exceptions.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = request("/api/v1/library/tracks");

    @Test
    void returnsApplicationErrorContract() {
        var response =
                handler.handle(new ApplicationException(ErrorCode.AUDIO_FILE_EMPTY, "Audio file is invalid"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).satisfies(problem -> {
            assertThat(problem.code()).isEqualTo("AUDIO_FILE_EMPTY");
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

    @Test
    void mapsMalformedRequestToBadRequest() {
        var response =
                handler.handleMalformedRequest(new HttpMessageNotReadableException("Malformed JSON", null), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(problem -> {
            assertThat(problem.code()).isEqualTo("MALFORMED_REQUEST");
            assertThat(problem.detail()).contains("could not be read");
        });
    }

    @Test
    void mapsMissingAuthenticationToUnauthorized() {
        var response = handler.handle(new AuthenticationRequiredException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(problem -> {
            assertThat(problem.code()).isEqualTo("AUTHENTICATION_REQUIRED");
            assertThat(problem.status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        });
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
