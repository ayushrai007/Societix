package com.societix.platform.identity.internal;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** Keeps local startup independent of Firebase credentials; claim-sync events retry until configured. */
@Component
@ConditionalOnExpression(
        "'${societix.security.firebase.admin.enabled:false}' != 'true' and "
                + "'${societix.security.firebase.auth-emulator-host:}' == ''")
class UnavailableFirebaseClaimsClient implements FirebaseClaimsClient {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public void setCustomClaims(String firebaseUid, Map<String, Object> claims) {
        throw new IllegalStateException("Firebase Admin SDK is disabled; custom claims were not synchronized");
    }
}
