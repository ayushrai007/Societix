package com.societix.platform.identity.internal;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Keeps local startup independent of Firebase credentials; claim-sync events retry until configured. */
@Component
@ConditionalOnMissingBean(FirebaseClaimsClient.class)
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
