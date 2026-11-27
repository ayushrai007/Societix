package com.societix.platform.identity.internal;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.internal.EmulatorCredentials;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression(
        "'${societix.security.firebase.admin.enabled:false}' == 'true' or "
                + "'${societix.security.firebase.auth-emulator-host:}' != ''")
class FirebaseAdminConfiguration {

    @Bean
    FirebaseApp societixFirebaseApp(
            @Value("${societix.security.firebase.project-id}") String projectId,
            @Value("${societix.security.firebase.auth-emulator-host:}") String emulatorHost)
            throws IOException {
        FirebaseOptions.Builder options = FirebaseOptions.builder().setProjectId(projectId);
        if (emulatorHost.isBlank()) {
            options.setCredentials(GoogleCredentials.getApplicationDefault());
        } else {
            String processEmulatorHost = System.getenv("FIREBASE_AUTH_EMULATOR_HOST");
            if (!emulatorHost.equals(processEmulatorHost)) {
                throw new IllegalStateException(
                        "FIREBASE_AUTH_EMULATOR_HOST must be exported in the backend process environment");
            }
            options.setCredentials(new EmulatorCredentials());
        }
        return FirebaseApp.initializeApp(options.build(), "societix");
    }

    @Bean
    FirebaseAuth societixFirebaseAuth(FirebaseApp societixFirebaseApp) {
        return FirebaseAuth.getInstance(societixFirebaseApp);
    }

    @Bean
    FirebaseClaimsClient firebaseClaimsClient(FirebaseAuth societixFirebaseAuth) {
        return (firebaseUid, claims) -> {
            try {
                societixFirebaseAuth.setCustomUserClaims(firebaseUid, claims);
            } catch (com.google.firebase.auth.FirebaseAuthException exception) {
                throw new IllegalStateException("Firebase custom-claims update failed", exception);
            }
        };
    }
}
