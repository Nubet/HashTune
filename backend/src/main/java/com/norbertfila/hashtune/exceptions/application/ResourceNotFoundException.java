package com.norbertfila.hashtune.exceptions.application;

import com.norbertfila.hashtune.exceptions.ErrorCode;

public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}
