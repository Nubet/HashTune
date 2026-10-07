package com.norbertfila.hashtune.adapter.in.security;

import com.norbertfila.hashtune.domain.identity.ExternalIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedIdentityResolver {
    public ExternalIdentity resolve(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException("A validated JWT is required");
        }

        var jwt = jwtAuthentication.getToken();
        return new ExternalIdentity(jwt.getIssuer().toString(), jwt.getSubject());
    }
}
