package com.societix.platform.identity.api;

import com.societix.platform.identity.domain.MembershipStatus;
import java.util.UUID;

public record OnboardingStatusResponse(
        UUID societyId,
        UUID membershipId,
        MembershipStatus status,
        boolean claimsSyncPending,
        boolean tokenRefreshRequired,
        Long authorizationVersion) {}
