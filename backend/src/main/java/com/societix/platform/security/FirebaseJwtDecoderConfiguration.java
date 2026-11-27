package com.societix.platform.security;

import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
class FirebaseJwtDecoderConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseJwtDecoderConfiguration.class);

    @Bean
    JwtDecoder firebaseJwtDecoder(
            @Value("${societix.security.firebase.project-id}") String projectId,
            @Value("${societix.security.firebase.jwk-set-uri}") String jwkSetUri,
            @Value("${societix.security.firebase.auth-emulator-host:}") String emulatorHost,
            ObjectProvider<FirebaseAuth> firebaseAuth) {
        if (projectId.isBlank()) {
            throw new IllegalStateException("Required property societix.security.firebase.project-id must not be blank");
        }
        String issuer = "https://securetoken.google.com/" + projectId;
        if (!emulatorHost.isBlank()) {
            if (!projectId.startsWith("demo-")) {
                throw new IllegalStateException(
                        "Firebase Auth Emulator requires a demo- project ID; refusing unsigned tokens for a real project");
            }
            FirebaseAuth emulator = firebaseAuth.getIfAvailable();
            if (emulator == null) {
                throw new IllegalStateException("Firebase Auth Emulator requires Firebase Admin to be enabled");
            }
            LOGGER.warn("Firebase Auth Emulator token verification is enabled for demo project {}", projectId);
            return new FirebaseAuthEmulatorJwtDecoder(emulator, projectId, issuer);
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(FirebaseJwtValidators.forProject(projectId, issuer));
        return decoder;
    }
}
