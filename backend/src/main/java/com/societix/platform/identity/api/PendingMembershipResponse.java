package com.societix.platform.identity.api;

import com.societix.platform.identity.domain.MembershipType;
import java.time.Instant;
import java.util.UUID;

public record PendingMembershipResponse(
        UUID membershipId, MembershipType membershipType, String verifiedPhoneNumber, Instant requestedAt) {}
