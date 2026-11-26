package com.societix.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class FirebaseSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder firebaseJwtDecoder,
            Converter<Jwt, AbstractAuthenticationToken> converter,
            @Value("${societix.security.society-creation-enabled:false}") boolean societyCreationEnabled)
            throws Exception {
        return http.authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/health", "/actuator/health").permitAll();
                    if (societyCreationEnabled) {
                        authorize.requestMatchers("/actuator/metrics/**").hasRole("PLATFORM_ADMIN");
                    } else {
                        authorize.requestMatchers("/actuator/metrics/**").denyAll();
                    }
                    authorize.requestMatchers("/actuator/**").denyAll();
                    if (societyCreationEnabled) {
                        authorize.requestMatchers(HttpMethod.POST, "/api/v1/societies").hasRole("PLATFORM_ADMIN");
                    } else {
                        authorize.requestMatchers(HttpMethod.POST, "/api/v1/societies").denyAll();
                    }
                    authorize.anyRequest().authenticated();
                })
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
            @Value("${societix.security.firebase.project-id}") String projectId,
            @Value("${societix.security.firebase.issuer-uri}") String issuer,
            @Value("${societix.security.firebase.jwk-set-uri}") String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(FirebaseJwtValidators.forProject(projectId, issuer));
        return decoder;
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> firebaseJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new FirebaseRoleAuthoritiesConverter());
        return converter;
    }
}
