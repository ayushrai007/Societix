package com.societix.platform.identity.api;

import com.societix.platform.identity.domain.MembershipStatus;
import java.util.Set;
import java.util.UUID;

public record MembershipResponse(
        UUID membershipId,
        MembershipStatus status,
        Set<String> roles,
        long authorizationVersion,
        boolean claimsSynchronized) {}
