package com.norbertfila.hashtune.exceptions.application;

import com.norbertfila.hashtune.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class ApplicationException extends RuntimeException {
    private final ErrorCode code;

    public ApplicationException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public HttpStatus status() {
        return code.status();
    }

    public String code() {
        return code.name();
    }
}
