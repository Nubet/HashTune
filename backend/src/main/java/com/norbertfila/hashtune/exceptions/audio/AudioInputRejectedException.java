package com.norbertfila.hashtune.exceptions.audio;

import com.norbertfila.hashtune.exceptions.ErrorCode;

public class AudioInputRejectedException extends RuntimeException {
    private final ErrorCode code;

    public AudioInputRejectedException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code.name();
    }
}
