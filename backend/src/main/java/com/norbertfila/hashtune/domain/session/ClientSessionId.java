package com.norbertfila.hashtune.domain.session;

public record ClientSessionId(String value) {
    public ClientSessionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be blank");
        }
    }
}
