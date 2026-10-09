package com.norbertfila.hashtune.exceptions.application;

import com.norbertfila.hashtune.exceptions.ErrorCode;

public class InvalidAudioException extends ApplicationException {
    public InvalidAudioException(ErrorCode code, String message) {
        super(code, message);
    }
}
