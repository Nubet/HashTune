package com.norbertfila.hashtune.security;

import com.norbertfila.hashtune.configuration.IdentityProviderProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final IdentityProviderProperties identityProvider;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        identityProvider.validate();
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/actuator/health/**").permitAll();
                    authorize.requestMatchers(
                                    HttpMethod.GET,
                                    "/api/v1/library/tracks",
                                    "/api/v1/library/tracks/albums",
                                    "/api/v1/library/tracks/artists",
                                    "/api/v1/library/tracks/artists/*/image",
                                    "/api/v1/library/tracks/*/cover")
                            .permitAll();
                    if (identityProvider.isEnabled()) {
                        authorize.anyRequest().authenticated();
                    } else {
                        authorize.anyRequest().permitAll();
                    }
                });

        if (identityProvider.isEnabled()) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        }

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(new JwtRolesGrantedAuthoritiesConverter());
        return converter;
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.identity-provider", name = "enabled", havingValue = "true")
    JwtDecoder jwtDecoder() {
        identityProvider.validate();

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(identityProvider.getJwkSetUri())
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(identityProvider.getIssuerUri());
        OAuth2TokenValidator<Jwt> validator = issuerValidator;
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud", audience -> audience != null && audience.contains(identityProvider.getAudience()));
        validator = new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
