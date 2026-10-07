package com.norbertfila.hashtune.adapter.in.security;

import com.norbertfila.hashtune.configuration.IdentityProviderProperties;
import com.norbertfila.hashtune.domain.identity.ExternalIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedIdentityResolver {
    private final IdentityProviderProperties identityProvider;

    public ExternalIdentity resolve(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            var jwt = jwtAuthentication.getToken();
            return new ExternalIdentity(jwt.getIssuer().toString(), jwt.getSubject());
        }

        if (identityProvider.isDevelopmentFallbackEnabled()) {
            return new ExternalIdentity(
                    identityProvider.getDevelopmentIssuer(), identityProvider.getDevelopmentSubject());
        }

        throw new IllegalStateException("A validated JWT is required");
    }
}
