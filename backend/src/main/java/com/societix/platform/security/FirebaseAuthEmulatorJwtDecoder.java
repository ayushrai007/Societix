package com.societix.platform.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;

/** Accepts unsigned ID tokens only through the explicitly configured Firebase Auth Emulator. */
final class FirebaseAuthEmulatorJwtDecoder implements JwtDecoder {

    private final FirebaseAuth firebaseAuth;
    private final String projectId;
    private final String issuer;

    FirebaseAuthEmulatorJwtDecoder(FirebaseAuth firebaseAuth, String projectId, String issuer) {
        this.firebaseAuth = firebaseAuth;
        this.projectId = projectId;
        this.issuer = issuer;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(token);
            Map<String, Object> claims = new HashMap<>(firebaseToken.getClaims());
            Object audience = claims.get("aud");
            if (!(audience instanceof String value && projectId.equals(value))
                    && !(audience instanceof Collection<?> values && values.contains(projectId))) {
                throw new JwtException("Firebase Auth Emulator token audience does not match project");
            }
            claims.put("aud", List.of(projectId));
            normalizeTimestamp(claims, "iat");
            normalizeTimestamp(claims, "exp");
            normalizeTimestamp(claims, "nbf");
            Jwt jwt = Jwt.withTokenValue(token)
                    .headers(headers -> headers.put("alg", "none"))
                    .claims(target -> target.putAll(claims))
                    .build();
            var validation = JwtValidators.createDefaultWithIssuer(issuer).validate(jwt);
            if (validation.hasErrors()) {
                throw new JwtException("Firebase Auth Emulator token issuer or lifetime is invalid");
            }
            return jwt;
        } catch (FirebaseAuthException | IllegalArgumentException exception) {
            throw new JwtException("Firebase Auth Emulator token is invalid", exception);
        }
    }

    private void normalizeTimestamp(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number timestamp) {
            claims.put(claimName, Instant.ofEpochSecond(timestamp.longValue()));
        }
    }

}
