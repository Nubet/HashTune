package com.norbertfila.hashtune.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.identity-provider")
public class IdentityProviderProperties {
    private boolean enabled;
    private boolean developmentFallbackEnabled;
    private String developmentIssuer;
    private String developmentSubject;
    private String issuerUri;
    private String jwkSetUri;
    private String audience;

    public void validate() {
        if (enabled == developmentFallbackEnabled) {
            throw new IllegalStateException(
                    "Exactly one identity provider mode must be enabled: JWT or development fallback");
        }
        if (!enabled) {
            return;
        }
        requireConfigured(issuerUri, "IDENTITY_PROVIDER_ISSUER_URI");
        requireConfigured(jwkSetUri, "IDENTITY_PROVIDER_JWK_SET_URI");
        requireConfigured(audience, "IDENTITY_PROVIDER_AUDIENCE");
    }

    private void requireConfigured(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " must be configured when JWT authentication is enabled");
        }
    }
}
