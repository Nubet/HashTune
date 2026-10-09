package com.norbertfila.hashtune.application.port.out;

public class AudioInputRejectedException extends RuntimeException {
    private final String code;

    public AudioInputRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
