package com.societix.platform.security;

import com.societix.platform.identity.api.RoleAssignmentBody;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Test fixture for proving role-based authorization without inventing a product endpoint. */
@RestController
class TreasurerTestController {

    @PostMapping("/api/v1/test/treasurer")
    @PreAuthorize("hasRole(T(com.societix.platform.identity.domain.Role).TREASURER.name())")
    ResponseEntity<Void> treasurerOnlyOperation(@RequestBody RoleAssignmentBody body) {
        return ResponseEntity.ok().build();
    }
}
