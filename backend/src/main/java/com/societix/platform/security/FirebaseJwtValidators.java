package com.societix.platform.security;

import java.time.Instant;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

/** Validation rules shared by production and Firebase Auth Emulator token decoders. */
final class FirebaseJwtValidators {

    private static final long AUTH_TIME_FUTURE_SKEW_SECONDS = 60;

    private FirebaseJwtValidators() {}

    static OAuth2TokenValidator<Jwt> forProject(String projectId, String issuer) {
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(projectId)
                ? OAuth2TokenValidatorResult.success()
                : failure("Firebase token audience does not match project");
        OAuth2TokenValidator<Jwt> subject = jwt -> jwt.getSubject() != null && !jwt.getSubject().isBlank()
                ? OAuth2TokenValidatorResult.success()
                : failure("Firebase token subject is required");
        OAuth2TokenValidator<Jwt> authenticationTime = jwt -> {
            Object claim = jwt.getClaim("auth_time");
            if (!(claim instanceof Number seconds)) {
                return failure("Firebase token auth_time must be a numeric timestamp");
            }
            double value = seconds.doubleValue();
            if (!Double.isFinite(value)
                    || value < 0
                    || value > Instant.now().getEpochSecond() + AUTH_TIME_FUTURE_SKEW_SECONDS) {
                return failure("Firebase token auth_time is outside the allowed range");
            }
            return OAuth2TokenValidatorResult.success();
        };
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audience, subject, authenticationTime);
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
