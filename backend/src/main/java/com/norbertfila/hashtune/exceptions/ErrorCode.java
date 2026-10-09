package com.norbertfila.hashtune.exceptions;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    AUDIO_FILE_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY),
    AUDIO_FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
    AUDIO_MIME_NOT_ALLOWED(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    AUDIO_COVER_ART_FORMAT_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_ENTITY),
    AUDIO_COVER_ART_TOO_LARGE(HttpStatus.UNPROCESSABLE_ENTITY),
    AUDIO_DECODE_TIMEOUT(HttpStatus.UNPROCESSABLE_ENTITY),
    AUDIO_DECODE_OUTPUT_TOO_LARGE(HttpStatus.UNPROCESSABLE_ENTITY),
    AUDIO_DURATION_TOO_LONG(HttpStatus.UNPROCESSABLE_ENTITY),
    AUDIO_FORMAT_NOT_SUPPORTED(HttpStatus.UNPROCESSABLE_ENTITY),
    AUDIO_PROBE_TIMEOUT(HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_AUDIO(HttpStatus.BAD_REQUEST),
    TRACK_ALREADY_EXISTS(HttpStatus.CONFLICT),
    TRACK_NOT_FOUND(HttpStatus.NOT_FOUND),
    COVER_ART_NOT_FOUND(HttpStatus.NOT_FOUND),
    RECORDING_NOT_FOUND(HttpStatus.NOT_FOUND),
    INDEXING_JOB_NOT_RETRYABLE(HttpStatus.CONFLICT),
    INDEXING_JOB_NOT_FOUND(HttpStatus.NOT_FOUND),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
