package com.societix.platform.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.societix.TestcontainersConfiguration;
import com.societix.platform.identity.api.MembershipResponse;
import com.societix.platform.identity.domain.MembershipStatus;
import com.societix.platform.identity.domain.MembershipType;
import com.societix.platform.identity.domain.Role;
import com.societix.platform.society.api.CreateSocietyRequest;
import com.societix.platform.society.internal.SocietyOnboardingService;
import com.societix.platform.outbox.OutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MembershipServiceIT {

    private final MembershipService memberships;
    private final MembershipAuthorizationService authorization;
    private final SocietyOnboardingService societies;
    private final FirebaseClaimsSyncHandler claimsSync;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactions;
    private final JsonMapper jsonMapper;
    private final MockMvc mockMvc;

    @MockitoBean
    private FirebaseClaimsClient firebaseClaims;

    @Autowired
    MembershipServiceIT(
            MembershipService memberships,
            MembershipAuthorizationService authorization,
            SocietyOnboardingService societies,
            FirebaseClaimsSyncHandler claimsSync,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            JsonMapper jsonMapper,
            MockMvc mockMvc) {
        this.memberships = memberships;
        this.authorization = authorization;
        this.societies = societies;
        this.claimsSync = claimsSync;
        this.jdbcTemplate = jdbcTemplate;
        this.transactions = new TransactionTemplate(transactionManager);
        this.jsonMapper = jsonMapper;
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void configureFirebaseClaimsClient() {
        org.mockito.Mockito.when(firebaseClaims.isConfigured()).thenReturn(true);
    }

    @Test
    void membershipApprovalSuspensionAndReactivationEnforceStatusAndVersions() {
        UUID societyId = createSociety();
        String firebaseUid = "lifecycle-user-" + UUID.randomUUID();
        MembershipResponse pending = memberships.requestMembership(
                firebaseUid, "+919876543210", societyId, MembershipType.RESIDENT);

        assertThat(pending.status()).isEqualTo(MembershipStatus.PENDING);
        assertThat(authorization.hasCurrentApprovedMembership(firebaseUid, identityId(firebaseUid), societyId, 0))
                .isFalse();

        MembershipResponse approved = memberships.approve(
                societyId, pending.membershipId(), Set.of(Role.OWNER), "secretary-uid");
        UUID userId = identityId(firebaseUid);
        assertThat(approved.status()).isEqualTo(MembershipStatus.APPROVED);
        assertThat(approved.authorizationVersion()).isEqualTo(1);
        assertThat(authorization.hasCurrentApprovedMembership(firebaseUid, userId, societyId, 1)).isTrue();
        assertThat(authorization.hasCurrentApprovedMembership(firebaseUid, userId, societyId, 0)).isFalse();

        MembershipResponse suspended = memberships.setSuspended(societyId, pending.membershipId(), true);
        assertThat(suspended.status()).isEqualTo(MembershipStatus.SUSPENDED);
        assertThat(suspended.authorizationVersion()).isEqualTo(2);
        assertThat(authorization.hasCurrentApprovedMembership(firebaseUid, userId, societyId, 1)).isFalse();
        assertThat(authorization.hasCurrentApprovedMembership(firebaseUid, userId, societyId, 2)).isFalse();

        MembershipResponse reactivated = memberships.setSuspended(societyId, pending.membershipId(), false);
        assertThat(reactivated.status()).isEqualTo(MembershipStatus.APPROVED);
        assertThat(reactivated.authorizationVersion()).isEqualTo(3);
        assertThat(authorization.hasCurrentApprovedMembership(firebaseUid, userId, societyId, 1)).isFalse();
        assertThat(authorization.hasCurrentApprovedMembership(firebaseUid, userId, societyId, 3)).isTrue();
    }

    @Test
    void staleSecretaryTokenIsRejectedByProtectedHttpEndpointAfterSuspension() throws Exception {
        UUID societyId = createSociety();
        String secretaryUid = "secretary-" + UUID.randomUUID();
        MembershipResponse requested = memberships.requestMembership(
                secretaryUid, "+919876543210", societyId, MembershipType.RESIDENT);
        MembershipResponse approved = memberships.approve(
                societyId, requested.membershipId(), Set.of(Role.SECRETARY), "platform-admin");
        String backupUid = "backup-secretary-" + UUID.randomUUID();
        MembershipResponse backupPending = memberships.requestMembership(
                backupUid, "+919876543211", societyId, MembershipType.RESIDENT);
        memberships.approve(
                societyId, backupPending.membershipId(), Set.of(Role.SECRETARY), secretaryUid);
        UUID userId = identityId(secretaryUid);
        var secretaryToken = jwt().jwt(token -> token
                        .subject(secretaryUid)
                        .claim("userId", userId.toString())
                        .claim("tenantId", societyId.toString())
                        .claim("authorizationVersion", approved.authorizationVersion())
                        .claim("membershipAuthorizationVersion", approved.authorizationVersion())
                        .claim("roles", List.of(Role.SECRETARY.name())))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SECRETARY"));

        mockMvc.perform(get("/api/v1/membership-requests/pending").with(secretaryToken))
                .andExpect(status().isOk());

        memberships.setSuspended(societyId, requested.membershipId(), true);
        mockMvc.perform(get("/api/v1/membership-requests/pending").with(secretaryToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void firstSecretaryAppointmentAndLastSecretaryProtectionWork() {
        UUID societyId = createSociety();
        String firstUid = "first-secretary-" + UUID.randomUUID();
        MembershipResponse firstPending = memberships.requestMembership(
                firstUid, "+919876543210", societyId, MembershipType.RESIDENT);
        assertThat(memberships.firstSecretaryCandidates(societyId))
                .extracting(candidate -> candidate.membershipId())
                .contains(firstPending.membershipId());

        MembershipResponse firstSecretary = memberships.appointFirstSecretary(societyId, firstPending.membershipId(), "platform-admin");
        assertThat(firstSecretary.status()).isEqualTo(MembershipStatus.APPROVED);
        assertThat(firstSecretary.roles()).containsExactlyInAnyOrder(Role.SECRETARY);

        assertThatThrownBy(() -> memberships.assignRoles(
                        societyId, firstPending.membershipId(), Set.of(Role.OWNER)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> memberships.setSuspended(societyId, firstPending.membershipId(), true))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        String secondUid = "second-resident-" + UUID.randomUUID();
        MembershipResponse secondPending = memberships.requestMembership(
                secondUid, "+919876543211", societyId, MembershipType.RESIDENT);
        MembershipResponse secondApproved = memberships.approve(
                societyId, secondPending.membershipId(), Set.of(Role.OWNER), "secretary-uid");
        assertThat(memberships
                        .assignRoles(societyId, secondPending.membershipId(), Set.of(Role.SECRETARY))
                        .authorizationVersion())
                .isEqualTo(secondApproved.authorizationVersion() + 1);
        assertThat(memberships.assignRoles(societyId, firstPending.membershipId(), Set.of(Role.OWNER)).roles())
                .containsExactlyInAnyOrder(Role.OWNER);
    }

    @Test
    void rejectionClearsPendingRequestAndMembershipClaimsSyncUsesCurrentDatabaseState() {
        UUID societyId = createSociety();
        String rejectedUid = "rejected-user-" + UUID.randomUUID();
        MembershipResponse rejectedRequest = memberships.requestMembership(
                rejectedUid, "+919876543210", societyId, MembershipType.RESIDENT);
        assertThat(memberships.reject(societyId, rejectedRequest.membershipId(), "secretary-uid").status())
                .isEqualTo(MembershipStatus.REJECTED);

        String approvedUid = "claims-user-" + UUID.randomUUID();
        MembershipResponse pending = memberships.requestMembership(
                approvedUid, "+919876543211", societyId, MembershipType.RESIDENT);
        MembershipResponse approved = memberships.approve(
                societyId, pending.membershipId(), Set.of(Role.OWNER), "secretary-uid");
        var payload = jsonMapper.createObjectNode().put("membershipId", pending.membershipId().toString());
        transactions.executeWithoutResult(transaction -> {
            jdbcTemplate.queryForObject(
                    "select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
            claimsSync.handle(new OutboxMessage(
                    UUID.randomUUID(), societyId, MembershipService.CLAIMS_SYNC_EVENT, payload, Instant.now()));
        });

        verify(firebaseClaims).setCustomClaims(
                org.mockito.ArgumentMatchers.eq(approvedUid),
                argThat(claims -> claims.get("userId").equals(identityId(approvedUid).toString())
                        && claims.get("tenantId").equals(societyId.toString())
                        && claims.get("membershipAuthorizationVersion").equals(approved.authorizationVersion())
                        && claims.get("roles").equals(List.of(Role.OWNER.name()))));
        assertThat(memberships.getMembership(societyId, pending.membershipId()).claimsSynchronized()).isTrue();
    }

    @Test
    void pendingMembershipClaimsHaveNoSocietyRoleOrTenantContext() {
        UUID societyId = createSociety();
        String firebaseUid = "pending-claims-user-" + UUID.randomUUID();
        MembershipResponse pending = memberships.requestMembership(
                firebaseUid, "+919876543210", societyId, MembershipType.RESIDENT);
        var payload = jsonMapper.createObjectNode().put("membershipId", pending.membershipId().toString());

        transactions.executeWithoutResult(transaction -> {
            jdbcTemplate.queryForObject(
                    "select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
            claimsSync.handle(new OutboxMessage(
                    UUID.randomUUID(), societyId, MembershipService.CLAIMS_SYNC_EVENT, payload, Instant.now()));
        });

        verify(firebaseClaims).setCustomClaims(
                org.mockito.ArgumentMatchers.eq(firebaseUid),
                argThat(claims -> claims.get("roles").equals(List.of())
                        && !claims.containsKey("tenantId")
                        && !claims.containsKey("membershipAuthorizationVersion")));
    }

    @Test
    void claimsSyncIgnoresUnknownStoredRolesAndContinues() {
        UUID societyId = createSociety();
        String firebaseUid = "unknown-role-claims-user-" + UUID.randomUUID();
        MembershipResponse pending = memberships.requestMembership(
                firebaseUid, "+919876543210", societyId, MembershipType.RESIDENT);
        MembershipResponse approved = memberships.approve(
                societyId, pending.membershipId(), Set.of(Role.OWNER), "secretary-uid");
        var payload = jsonMapper.createObjectNode().put("membershipId", pending.membershipId().toString());

        transactions.executeWithoutResult(transaction -> {
            jdbcTemplate.queryForObject(
                    "select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
            jdbcTemplate.update(
                    "UPDATE society_membership SET roles = ?::jsonb WHERE tenant_id = ? AND id = ?",
                    "[\"OWNER\",\"FUTURE_ROLE\"]",
                    societyId,
                    pending.membershipId());
            claimsSync.handle(new OutboxMessage(
                    UUID.randomUUID(), societyId, MembershipService.CLAIMS_SYNC_EVENT, payload, Instant.now()));
        });

        verify(firebaseClaims).setCustomClaims(
                org.mockito.ArgumentMatchers.eq(firebaseUid),
                argThat(claims -> claims.get("roles").equals(List.of(Role.OWNER.name()))));
        assertThat(memberships.getMembership(societyId, pending.membershipId()).roles())
                .containsExactlyInAnyOrder(Role.OWNER);
        assertThat(approved.authorizationVersion()).isEqualTo(1);
    }

    @Test
    void roleAssignmentEndpointRejectsPlatformAdminAndUnknownRoles() throws Exception {
        UUID societyId = createSociety();
        String secretaryUid = "role-assignment-secretary-" + UUID.randomUUID();
        MembershipResponse secretaryPending = memberships.requestMembership(
                secretaryUid, "+919876543210", societyId, MembershipType.RESIDENT);
        MembershipResponse secretary = memberships.approve(
                societyId, secretaryPending.membershipId(), Set.of(Role.SECRETARY), "platform-admin");

        String residentUid = "role-assignment-resident-" + UUID.randomUUID();
        MembershipResponse residentPending = memberships.requestMembership(
                residentUid, "+919876543211", societyId, MembershipType.RESIDENT);
        memberships.approve(societyId, residentPending.membershipId(), Set.of(Role.OWNER), secretaryUid);

        RequestPostProcessor secretaryToken = jwt()
                .jwt(token -> token.subject(secretaryUid)
                        .claim("userId", identityId(secretaryUid).toString())
                        .claim("tenantId", societyId.toString())
                        .claim("authorizationVersion", secretary.authorizationVersion())
                        .claim("membershipAuthorizationVersion", secretary.authorizationVersion())
                        .claim("roles", List.of(Role.SECRETARY.name())))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        Role.SECRETARY.authority()));

        mockMvc.perform(put("/api/v1/memberships/{membershipId}/roles", residentPending.membershipId())
                        .with(secretaryToken)
                        .contentType("application/json")
                        .content("{\"roles\":[\"PLATFORM_ADMIN\"]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/memberships/{membershipId}/roles", residentPending.membershipId())
                        .with(secretaryToken)
                        .contentType("application/json")
                        .content("{\"roles\":[\"UNKNOWN_ROLE\"]}"))
                .andExpect(status().isBadRequest());
    }

    private UUID createSociety() {
        return societies.onboard(new CreateSocietyRequest(
                        "Membership Test " + UUID.randomUUID(), "1 Test Road", "MH"))
                .getId();
    }

    private UUID identityId(String firebaseUid) {
        return jdbcTemplate.queryForObject("SELECT id FROM identity_account WHERE firebase_uid = ?", UUID.class, firebaseUid);
    }
}
