package com.norbertfila.hashtune.domain.identity;

public record ExternalIdentity(String issuer, String subject) {
    public ExternalIdentity {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("Identity issuer cannot be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Identity subject cannot be blank");
        }
    }
}
