package com.societix.platform.identity.domain;

import java.util.Optional;

/** Societix roles carried in validated Firebase claims and checked by backend authorization. */
public enum Role {
    PLATFORM_ADMIN,
    SECRETARY,
    TREASURER,
    COMMITTEE_MEMBER,
    OWNER,
    TENANT,
    GUARD,
    AUDITOR;

    public String authority() {
        return "ROLE_" + name();
    }

    public boolean isAssignableToSocietyMembership() {
        return this != PLATFORM_ADMIN;
    }

    public static Optional<Role> fromClaim(String roleName) {
        if (roleName == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(roleName));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
