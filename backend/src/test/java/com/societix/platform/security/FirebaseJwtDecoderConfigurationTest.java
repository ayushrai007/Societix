package com.societix.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class FirebaseJwtDecoderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FirebaseJwtDecoderConfiguration.class)
            .withPropertyValues(
                    "societix.security.firebase.issuer-uri=https://securetoken.google.com/societix-test",
                    "societix.security.firebase.jwk-set-uri=https://example.test/jwks");

    @Test
    void missingFirebaseProjectIdFailsApplicationStartup() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasMessageContaining("societix.security.firebase.project-id");
        });
    }

    @Test
    void configuredFirebaseProjectCreatesJwtDecoder() {
        contextRunner.withPropertyValues("societix.security.firebase.project-id=societix-test")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                });
    }
}
