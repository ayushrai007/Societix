package com.societix.platform.identity.api;

import com.societix.platform.identity.domain.MembershipStatus;
import com.societix.platform.identity.domain.MembershipType;
import java.util.Set;
import java.util.UUID;

public record ManagedMembershipResponse(
        UUID membershipId,
        MembershipType membershipType,
        MembershipStatus status,
        String verifiedPhoneNumber,
        Set<String> roles,
        long authorizationVersion) {}
