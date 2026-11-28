package com.societix.platform.identity.api;

import com.societix.platform.identity.domain.MembershipStatus;
import com.societix.platform.identity.domain.Role;
import java.util.Set;
import java.util.UUID;

public record MembershipResponse(
        UUID membershipId,
        MembershipStatus status,
        Set<Role> roles,
        long authorizationVersion,
        boolean claimsSynchronized) {}
