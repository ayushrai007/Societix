package com.societix.platform.identity.internal;

import java.util.Map;

/** Writes a complete Societix-managed custom-claims document for one Firebase identity. */
public interface FirebaseClaimsClient {

    default boolean isConfigured() {
        return true;
    }

    void setCustomClaims(String firebaseUid, Map<String, Object> claims);
}
