package com.societix.platform.identity.api;

import com.societix.platform.identity.internal.MembershipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MembershipAdministrationController {

    private final MembershipService memberships;

    public MembershipAdministrationController(MembershipService memberships) {
        this.memberships = memberships;
    }

    @GetMapping("/membership-requests/pending")
    @PreAuthorize("hasRole(T(com.societix.platform.identity.domain.Role).SECRETARY.name())")
    public List<PendingMembershipResponse> pendingMemberships(@AuthenticationPrincipal Jwt jwt) {
        return memberships.pendingMemberships(tenantId(jwt));
    }

    @GetMapping("/memberships")
    @PreAuthorize("hasRole(T(com.societix.platform.identity.domain.Role).SECRETARY.name())")
    public List<ManagedMembershipResponse> approvedMemberships(@AuthenticationPrincipal Jwt jwt) {
        return memberships.approvedMemberships(tenantId(jwt));
    }

    @GetMapping("/societies/{societyId}/first-secretary-candidates")
    @PreAuthorize("hasRole(T(com.societix.platform.identity.domain.Role).PLATFORM_ADMIN.name())")
    public List<PendingMembershipResponse> firstSecretaryCandidates(@PathVariable UUID societyId) {
        return memberships.firstSecretaryCandidates(societyId);
    }

    @PostMapping("/societies/{societyId}/membership-requests/{membershipId}/appoint-first-secretary")
    @PreAuthorize("hasRole(T(com.societix.platform.identity.domain.Role).PLATFORM_ADMIN.name())")
    public MembershipResponse appointFirstSecretary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID societyId,
            @PathVariable UUID membershipId) {
        return memberships.appointFirstSecretary(societyId, membershipId, jwt.getSubject());
    }

    @PostMapping("/membership-requests/{membershipId}/approve")
    @PreAuthorize("hasRole(T(com.societix.platform.identity.domain.Role).SECRETARY.name())")
    public MembershipResponse approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID membershipId,
            @Valid @RequestBody RoleAssignmentBody body) {
        return memberships.approve(tenantId(jwt), membershipId, body.roles(), jwt.getSubject());
    }

    @PostMapping("/membership-requests/{membershipId}/reject")
    @PreAuthorize("hasRole(T(com.societix.platform.identity.domain.Role).SECRETARY.name())")
    public MembershipResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID membershipId) {
        return memberships.reject(tenantId(jwt), membershipId, jwt.getSubject());
    }

    @PutMapping("/memberships/{membershipId}/roles")
    @PreAuthorize("hasRole(T(com.societix.platform.identity.domain.Role).SECRETARY.name())")
    public MembershipResponse assignRoles(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID membershipId,
            @Valid @RequestBody RoleAssignmentBody body) {
        return memberships.assignRoles(tenantId(jwt), membershipId, body.roles());
    }

    @PutMapping("/memberships/{membershipId}/suspension")
    @PreAuthorize("hasRole(T(com.societix.platform.identity.domain.Role).SECRETARY.name())")
    public MembershipResponse setSuspended(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID membershipId,
            @RequestBody SuspensionBody body) {
        return memberships.setSuspended(tenantId(jwt), membershipId, body.suspended());
    }

    private UUID tenantId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("tenantId"));
    }

    public record SuspensionBody(boolean suspended) {}
}
