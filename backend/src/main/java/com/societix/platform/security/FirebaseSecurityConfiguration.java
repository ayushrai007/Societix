package com.societix.platform.security;

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class FirebaseSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtDecoder firebaseJwtDecoder, Converter<Jwt, AbstractAuthenticationToken> converter)
            throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/health", "/actuator/health").permitAll()
                        .requestMatchers("/actuator/metrics/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers("/actuator/**").denyAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(firebaseJwtDecoder).jwtAuthenticationConverter(converter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .formLogin(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .csrf(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    JwtDecoder firebaseJwtDecoder(
            @Value("${societix.security.firebase.project-id:societix-local}") String projectId,
            @Value("${societix.security.firebase.issuer-uri}") String issuer,
            @Value("${societix.security.firebase.jwk-set-uri}") String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerAndTime = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(projectId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Firebase token audience does not match project", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerAndTime, audience));
        return decoder;
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> firebaseJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object roles = jwt.getClaim("roles");
            if (!(roles instanceof Collection<?> values)) {
                return List.of();
            }
            return values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(role -> role.matches("[A-Z][A-Z0-9_]*"))
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });
        return converter;
    }
}
