package com.societix.platform.identity.api;

import com.societix.platform.identity.domain.MembershipStatus;
import com.societix.platform.identity.domain.MembershipType;
import com.societix.platform.identity.domain.Role;
import java.util.Set;
import java.util.UUID;

public record ManagedMembershipResponse(
        UUID membershipId,
        MembershipType membershipType,
        MembershipStatus status,
        String verifiedPhoneNumber,
        Set<Role> roles,
        long authorizationVersion) {}
