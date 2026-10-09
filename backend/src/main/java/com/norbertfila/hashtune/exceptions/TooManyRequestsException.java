package com.norbertfila.hashtune.exceptions;

import java.time.Duration;
import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApplicationException {
    private final Duration retryAfter;

    public TooManyRequestsException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many requests. Try again later.");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
