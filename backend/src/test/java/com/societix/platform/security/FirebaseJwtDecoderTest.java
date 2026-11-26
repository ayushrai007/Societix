package com.societix.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class FirebaseJwtDecoderTest {

    private static final String PROJECT_ID = "societix-test";
    private static final String ISSUER = "https://securetoken.google.com/" + PROJECT_ID;
    private static KeyPair keyPair;

    @BeforeAll
    static void createSigningKey() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void acceptsSignedFirebaseTokenWithRequiredClaims() throws Exception {
        var decoded = decoder().decode(token(claims(
                ISSUER, PROJECT_ID, "firebase-uid", Instant.now(), Instant.now().minusSeconds(45).getEpochSecond())));
        Object authenticationTime = decoded.getClaim("auth_time");

        assertThat(decoded.getSubject()).isEqualTo("firebase-uid");
        assertThat(authenticationTime).isInstanceOf(Number.class);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        assertRejected(claims(
                ISSUER,
                PROJECT_ID,
                "firebase-uid",
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(45).getEpochSecond()));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        assertRejected(claims(
                "https://securetoken.google.com/another-project",
                PROJECT_ID,
                "firebase-uid",
                Instant.now(),
                Instant.now().minusSeconds(45).getEpochSecond()));
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        assertRejected(claims(
                ISSUER, "another-project", "firebase-uid", Instant.now(), Instant.now().minusSeconds(45).getEpochSecond()));
    }

    @Test
    void rejectsMissingSubject() throws Exception {
        assertRejected(claims(
                ISSUER, PROJECT_ID, null, Instant.now(), Instant.now().minusSeconds(45).getEpochSecond()));
    }

    @Test
    void rejectsMissingAuthenticationTime() throws Exception {
        assertRejected(claims(ISSUER, PROJECT_ID, "firebase-uid", Instant.now(), null));
    }

    @Test
    void rejectsNonNumericAuthenticationTime() throws Exception {
        JWTClaimsSet claims = claims(ISSUER, PROJECT_ID, "firebase-uid", Instant.now(), "recent");
        assertRejected(claims);
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> decoder().decode("not-a-valid-jwt")).isInstanceOf(JwtException.class);
    }

    private void assertRejected(JWTClaimsSet claims) throws Exception {
        assertThatThrownBy(() -> decoder().decode(token(claims))).isInstanceOf(JwtException.class);
    }

    private JwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        decoder.setJwtValidator(FirebaseJwtValidators.forProject(PROJECT_ID, ISSUER));
        return decoder;
    }

    private String token(JWTClaimsSet claims) throws JOSEException {
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signed.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        return signed.serialize();
    }

    private JWTClaimsSet claims(
            String issuer, String audience, String subject, Instant referenceTime, Object authTime) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now.minusSeconds(30)))
                .notBeforeTime(Date.from(now.minusSeconds(30)))
                .expirationTime(Date.from(referenceTime.plusSeconds(300)));
        if (subject != null) {
            builder.subject(subject);
        }
        if (authTime != null) {
            builder.claim("auth_time", authTime);
        }
        return builder.build();
    }
}
