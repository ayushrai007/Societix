package com.societix.platform.identity.api;

import com.societix.platform.identity.domain.Role;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record RoleAssignmentBody(@NotEmpty Set<Role> roles) {}
