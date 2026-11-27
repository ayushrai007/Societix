package com.societix.platform.identity.api;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record RoleAssignmentBody(@NotEmpty Set<String> roles) {}
