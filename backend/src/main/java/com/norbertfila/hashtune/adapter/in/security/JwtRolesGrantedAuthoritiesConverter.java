package com.norbertfila.hashtune.adapter.in.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtRolesGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private static final String CLAIM_NAME = "roles";
    private static final String AUTHORITY_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object claim = jwt.getClaims().get(CLAIM_NAME);
        if (claim instanceof Collection<?> roles) {
            return roles.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(this::toAuthority)
                    .filter(Objects::nonNull)
                    .toList();
        }
        if (claim instanceof String role) {
            GrantedAuthority authority = toAuthority(role);
            return authority == null ? Collections.emptyList() : List.of(authority);
        }
        return Collections.emptyList();
    }

    private GrantedAuthority toAuthority(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return new SimpleGrantedAuthority(AUTHORITY_PREFIX + role.trim().toUpperCase(Locale.ROOT));
    }
}
