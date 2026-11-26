package com.societix.platform.identity.domain;

import java.util.Set;

/** Temporary claim-name contract; SOC-12 will replace these names with its Role enum. */
public final class SocietixRoleClaims {

    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    public static final String SECRETARY = "SECRETARY";
    public static final String TREASURER = "TREASURER";
    public static final String COMMITTEE_MEMBER = "COMMITTEE_MEMBER";
    public static final String OWNER = "OWNER";
    public static final String TENANT = "TENANT";
    public static final String GUARD = "GUARD";
    public static final String AUDITOR = "AUDITOR";

    private static final Set<String> ASSIGNABLE_ROLES = Set.of(
            SECRETARY,
            TREASURER,
            COMMITTEE_MEMBER,
            OWNER,
            TENANT,
            GUARD,
            AUDITOR);

    private SocietixRoleClaims() {}

    public static boolean canBeAssigned(String role) {
        return ASSIGNABLE_ROLES.contains(role);
    }
}
