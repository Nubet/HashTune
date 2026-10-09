package com.norbertfila.hashtune.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesGrantedAuthoritiesConverterTest {
    private final JwtRolesGrantedAuthoritiesConverter converter = new JwtRolesGrantedAuthoritiesConverter();

    @Test
    void mapsRolesToUppercaseRoleAuthorities() {
        Jwt jwt = jwtWithRoles(List.of("admin", "operator"));

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "ROLE_OPERATOR");
    }

    @Test
    void ignoresMissingRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject")
                .issuer("https://issuer.example")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private Jwt jwtWithRoles(List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject")
                .issuer("https://issuer.example")
                .claim("roles", roles)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
