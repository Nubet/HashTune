package com.norbertfila.hashtune.exceptions.application;

import com.norbertfila.hashtune.exceptions.ErrorCode;
import java.time.Duration;

public class TooManyRequestsException extends ApplicationException {
    private final Duration retryAfter;

    public TooManyRequestsException(Duration retryAfter) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, "Too many requests. Try again later.");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
