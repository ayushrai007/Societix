package com.societix.platform.identity.api;

import com.societix.platform.identity.domain.MembershipType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MembershipRequestBody(@NotNull UUID societyId, @NotNull MembershipType membershipType) {}
