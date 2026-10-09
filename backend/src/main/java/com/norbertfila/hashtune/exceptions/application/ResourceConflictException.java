package com.norbertfila.hashtune.exceptions.application;

import com.norbertfila.hashtune.exceptions.ErrorCode;

public class ResourceConflictException extends ApplicationException {
    public ResourceConflictException(ErrorCode code, String message) {
        super(code, message);
    }
}
