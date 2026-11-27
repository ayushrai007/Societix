package com.societix.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
class FirebaseJwtDecoderConfiguration {

    @Bean
    JwtDecoder firebaseJwtDecoder(
            @Value("${societix.security.firebase.project-id}") String projectId,
            @Value("${societix.security.firebase.jwk-set-uri}") String jwkSetUri) {
        if (projectId.isBlank()) {
            throw new IllegalStateException("Required property societix.security.firebase.project-id must not be blank");
        }
        String issuer = "https://securetoken.google.com/" + projectId;
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(FirebaseJwtValidators.forProject(projectId, issuer));
        return decoder;
    }
}
