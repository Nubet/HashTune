package com.norbertfila.hashtune.exceptions.application;

import com.norbertfila.hashtune.exceptions.ErrorCode;

public class ApplicationException extends RuntimeException {
    private final ErrorCode code;

    public ApplicationException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public org.springframework.http.HttpStatus status() {
        return code.status();
    }

    public String code() {
        return code.name();
    }
}
