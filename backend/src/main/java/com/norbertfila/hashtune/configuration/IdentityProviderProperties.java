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
}
