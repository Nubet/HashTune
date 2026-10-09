package com.norbertfila.hashtune.security;

import com.norbertfila.hashtune.configuration.IdentityProviderProperties;
import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedIdentityResolver {
    private final IdentityProviderProperties identityProvider;

    public Optional<ExternalIdentity> resolveOptional(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            var jwt = jwtAuthentication.getToken();
            return Optional.of(new ExternalIdentity(jwt.getIssuer().toString(), jwt.getSubject()));
        }
        if (identityProvider.isDevelopmentFallbackEnabled()) {
            return Optional.of(new ExternalIdentity(
                    identityProvider.getDevelopmentIssuer(), identityProvider.getDevelopmentSubject()));
        }
        return Optional.empty();
    }

    public ExternalIdentity resolve(Authentication authentication) {
        return resolveOptional(authentication)
                .orElseThrow(() -> new IllegalStateException("A validated JWT is required"));
    }
}
