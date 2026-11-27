package com.societix.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class FirebaseJwtDecoderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FirebaseJwtDecoderConfiguration.class)
            .withPropertyValues(
                    "societix.security.firebase.jwk-set-uri=https://example.test/jwks",
                    "societix.security.firebase.project-id=");

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

    @Test
    void emulatorCannotBeEnabledForARealFirebaseProject() {
        contextRunner.withPropertyValues(
                        "societix.security.firebase.project-id=societix-production",
                        "societix.security.firebase.auth-emulator-host=127.0.0.1:9099")
                .withBean(FirebaseAuth.class, () -> mock(FirebaseAuth.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("requires a demo- project ID");
                });
    }

    @Test
    void demoProjectCanUseTheConfiguredAuthEmulator() {
        contextRunner.withPropertyValues(
                        "societix.security.firebase.project-id=demo-societix-local",
                        "societix.security.firebase.auth-emulator-host=127.0.0.1:9099")
                .withBean(FirebaseAuth.class, () -> mock(FirebaseAuth.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtDecoder.class)).isInstanceOf(FirebaseAuthEmulatorJwtDecoder.class);
                });
    }
}
