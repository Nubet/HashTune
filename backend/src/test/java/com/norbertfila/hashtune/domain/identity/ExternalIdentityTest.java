package com.norbertfila.hashtune.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExternalIdentityTest {
    @Test
    void storesIssuerAndSubject() {
        ExternalIdentity identity = new ExternalIdentity("https://issuer.example", "subject-123");

        assertThat(identity.issuer()).isEqualTo("https://issuer.example");
        assertThat(identity.subject()).isEqualTo("subject-123");
    }

    @Test
    void rejectsBlankIssuer() {
        assertThatThrownBy(() -> new ExternalIdentity(" ", "subject-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Identity issuer cannot be blank");
    }

    @Test
    void rejectsBlankSubject() {
        assertThatThrownBy(() -> new ExternalIdentity("https://issuer.example", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Identity subject cannot be blank");
    }
}
