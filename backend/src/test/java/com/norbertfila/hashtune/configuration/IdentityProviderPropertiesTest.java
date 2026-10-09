package com.norbertfila.hashtune.configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdentityProviderPropertiesTest {
    @Test
    void acceptsDevelopmentFallbackMode() {
        IdentityProviderProperties properties = new IdentityProviderProperties();
        properties.setDevelopmentFallbackEnabled(true);

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void acceptsJwtModeWithRequiredConfiguration() {
        IdentityProviderProperties properties = new IdentityProviderProperties();
        properties.setEnabled(true);
        properties.setIssuerUri("https://issuer.example.com");
        properties.setJwkSetUri("https://issuer.example.com/.well-known/jwks.json");
        properties.setAudience("authenticated");

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsAmbiguousMode() {
        IdentityProviderProperties properties = new IdentityProviderProperties();

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one identity provider mode");
    }

    @Test
    void rejectsJwtModeWithoutRequiredConfiguration() {
        IdentityProviderProperties properties = new IdentityProviderProperties();
        properties.setEnabled(true);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IDENTITY_PROVIDER_ISSUER_URI");
    }
}
