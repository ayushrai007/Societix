package com.societix.platform.identity.internal;

import com.societix.platform.identity.api.MembershipResponse;
import com.societix.platform.identity.api.ManagedMembershipResponse;
import com.societix.platform.identity.api.OnboardingStatusResponse;
import com.societix.platform.identity.api.PendingMembershipResponse;
import com.societix.platform.identity.domain.MembershipStatus;
import com.societix.platform.identity.domain.MembershipType;
import com.societix.platform.identity.domain.PhoneNumberProtector;
import com.societix.platform.identity.domain.Role;
import com.societix.platform.identity.domain.TenantSql;
import com.societix.platform.outbox.OutboxEvent;
import com.societix.platform.outbox.OutboxWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class MembershipService {

    static final String CLAIMS_SYNC_EVENT = "platform.membership.claims.sync";

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;
    private final PhoneNumberProtector phoneNumberProtector;
    private final TenantSql tenantSql;
    private final OutboxWriter outboxWriter;
    private final FirebaseClaimsClient firebaseClaimsClient;

    public MembershipService(
            JdbcTemplate jdbcTemplate,
            JsonMapper jsonMapper,
            PhoneNumberProtector phoneNumberProtector,
            TenantSql tenantSql,
            OutboxWriter outboxWriter,
            FirebaseClaimsClient firebaseClaimsClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
        this.phoneNumberProtector = phoneNumberProtector;
        this.tenantSql = tenantSql;
        this.outboxWriter = outboxWriter;
        this.firebaseClaimsClient = firebaseClaimsClient;
    }

    @Transactional
    public MembershipResponse requestMembership(
            String firebaseUid, String verifiedPhoneNumber, UUID societyId, MembershipType membershipType) {
        requirePhoneProtectionConfigured();
        byte[] encryptedPhone = phoneNumberProtector.encrypt(verifiedPhoneNumber);
        tenantSql.setTenant(societyId);
        if (jdbcTemplate.queryForObject("SELECT count(*) FROM society WHERE id = ?", Integer.class, societyId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Society was not found");
        }

        UUID identityId = jdbcTemplate.queryForObject(
                """
                INSERT INTO identity_account (id, firebase_uid, phone_number_cipher)
                VALUES (?, ?, ?)
                ON CONFLICT (firebase_uid) DO UPDATE SET phone_number_cipher = EXCLUDED.phone_number_cipher
                RETURNING id
                """,
                UUID.class,
                UUID.randomUUID(),
                firebaseUid,
                encryptedPhone);
        UUID membershipId = UUID.randomUUID();
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO society_membership (id, tenant_id, identity_id, membership_type)
                    VALUES (?, ?, ?, ?)
                    """,
                    membershipId,
                    societyId,
                    identityId,
                    membershipType.name());
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A membership request for this account is already active", exception);
        }
        return getMembership(societyId, membershipId);
    }

    @Transactional(readOnly = true)
    public OnboardingStatusResponse onboardingStatus(
            String firebaseUid, UUID societyId, Long tokenAuthorizationVersion) {
        tenantSql.setTenant(societyId);
        List<OnboardingStatusResponse> results = jdbcTemplate.query(
                """
                SELECT sm.id, sm.status, sm.authorization_version, sm.claims_sync_version
                FROM society_membership sm
                JOIN identity_account ia ON ia.id = sm.identity_id
                WHERE sm.tenant_id = ? AND ia.firebase_uid = ?
                """,
                (rs, rowNum) -> {
                    MembershipStatus status = MembershipStatus.valueOf(rs.getString("status"));
                    long authorizationVersion = rs.getLong("authorization_version");
                    boolean claimsSyncPending = rs.getLong("claims_sync_version") < authorizationVersion;
                    boolean refreshRequired = (status == MembershipStatus.APPROVED || status == MembershipStatus.SUSPENDED)
                            && (tokenAuthorizationVersion == null
                                    || tokenAuthorizationVersion.longValue() != authorizationVersion);
                    return new OnboardingStatusResponse(
                            societyId,
                            rs.getObject("id", UUID.class),
                            status,
                            claimsSyncPending,
                            refreshRequired,
                            authorizationVersion);
                },
                societyId,
                firebaseUid);
        if (results.isEmpty()) {
            return new OnboardingStatusResponse(societyId, null, null, false, false, null);
        }
        return results.getFirst();
    }

    @Transactional(readOnly = true)
    public List<PendingMembershipResponse> pendingMemberships(UUID societyId) {
        requirePhoneProtectionConfigured();
        tenantSql.setTenant(societyId);
        return jdbcTemplate.query(
                """
                SELECT sm.id, sm.membership_type, ia.phone_number_cipher, sm.requested_at
                FROM society_membership sm
                JOIN identity_account ia ON ia.id = sm.identity_id
                WHERE sm.tenant_id = ? AND sm.status = 'PENDING'
                ORDER BY sm.requested_at, sm.id
                """,
                (rs, rowNum) -> new PendingMembershipResponse(
                        rs.getObject("id", UUID.class),
                        MembershipType.valueOf(rs.getString("membership_type")),
                        phoneNumberProtector.decrypt(rs.getBytes("phone_number_cipher")),
                        rs.getTimestamp("requested_at").toInstant()),
                societyId);
    }

    @Transactional(readOnly = true)
    public List<ManagedMembershipResponse> approvedMemberships(UUID societyId) {
        requirePhoneProtectionConfigured();
        tenantSql.setTenant(societyId);
        return jdbcTemplate.query(
                """
                SELECT sm.id, sm.membership_type, sm.status, sm.roles::text AS roles,
                       sm.authorization_version, ia.phone_number_cipher
                FROM society_membership sm
                JOIN identity_account ia ON ia.id = sm.identity_id
                WHERE sm.tenant_id = ? AND sm.status IN ('APPROVED', 'SUSPENDED')
                ORDER BY sm.membership_type, sm.requested_at, sm.id
                """,
                (rs, rowNum) -> new ManagedMembershipResponse(
                        rs.getObject("id", UUID.class),
                        MembershipType.valueOf(rs.getString("membership_type")),
                        MembershipStatus.valueOf(rs.getString("status")),
                        phoneNumberProtector.decrypt(rs.getBytes("phone_number_cipher")),
                        readRoles(rs.getString("roles")),
                        rs.getLong("authorization_version")),
                societyId);
    }

    @Transactional(readOnly = true)
    public List<PendingMembershipResponse> firstSecretaryCandidates(UUID societyId) {
        requirePhoneProtectionConfigured();
        tenantSql.setTenant(societyId);
        if (jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM society WHERE id = ? AND initial_secretary_assigned_at IS NULL",
                        Integer.class,
                        societyId)
                == 0) {
            return List.of();
        }
        return jdbcTemplate.query(
                """
                SELECT sm.id, sm.membership_type, ia.phone_number_cipher, sm.requested_at
                FROM society_membership sm
                JOIN identity_account ia ON ia.id = sm.identity_id
                WHERE sm.tenant_id = ? AND sm.status = 'PENDING' AND sm.membership_type = 'RESIDENT'
                ORDER BY sm.requested_at, sm.id
                """,
                (rs, rowNum) -> new PendingMembershipResponse(
                        rs.getObject("id", UUID.class),
                        MembershipType.valueOf(rs.getString("membership_type")),
                        phoneNumberProtector.decrypt(rs.getBytes("phone_number_cipher")),
                        rs.getTimestamp("requested_at").toInstant()),
                societyId);
    }

    @Transactional
    public MembershipResponse appointFirstSecretary(UUID societyId, UUID membershipId, String platformAdminUid) {
        requireFirebaseClaimsSyncConfigured();
        tenantSql.setTenant(societyId);
        MembershipType type = requirePendingMembership(societyId, membershipId);
        if (type != MembershipType.RESIDENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The first secretary must be a resident applicant");
        }
        String candidateUid = jdbcTemplate.queryForObject(
                """
                SELECT ia.firebase_uid FROM society_membership sm
                JOIN identity_account ia ON ia.id = sm.identity_id
                WHERE sm.tenant_id = ? AND sm.id = ? AND sm.status = 'PENDING'
                """,
                String.class,
                societyId,
                membershipId);
        if (platformAdminUid.equals(candidateUid)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Platform administrators cannot appoint themselves");
        }
        int assigned = jdbcTemplate.update(
                "UPDATE society SET initial_secretary_assigned_at = now() WHERE id = ? AND initial_secretary_assigned_at IS NULL",
                societyId);
        if (assigned != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The initial society secretary is already appointed");
        }
        approve(societyId, membershipId, Set.of(Role.SECRETARY), platformAdminUid);
        return getMembership(societyId, membershipId);
    }

    @Transactional
    public MembershipResponse approve(
            UUID societyId, UUID membershipId, Set<Role> roles, String approverFirebaseUid) {
        requireFirebaseClaimsSyncConfigured();
        tenantSql.setTenant(societyId);
        MembershipType membershipType = requirePendingMembership(societyId, membershipId);
        Set<Role> assignedRoles = validateRoles(membershipType, roles);
        String roleJson = writeJson(assignedRoles);
        List<ChangedMembership> changed = jdbcTemplate.query(
                """
                UPDATE society_membership
                SET status = 'APPROVED', roles = ?::jsonb, authorization_version = authorization_version + 1,
                    decided_at = now(), decided_by_firebase_uid = ?
                WHERE id = ? AND tenant_id = ? AND status = 'PENDING'
                RETURNING identity_id, authorization_version
                """,
                (rs, rowNum) -> new ChangedMembership(rs.getObject("identity_id", UUID.class), rs.getLong("authorization_version")),
                roleJson,
                approverFirebaseUid,
                membershipId,
                societyId);
        if (changed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Membership request is no longer pending");
        }
        appendClaimsSync(societyId, membershipId);
        return getMembership(societyId, membershipId);
    }

    @Transactional
    public MembershipResponse reject(UUID societyId, UUID membershipId, String approverFirebaseUid) {
        tenantSql.setTenant(societyId);
        int updated = jdbcTemplate.update(
                """
                UPDATE society_membership
                SET status = 'REJECTED', roles = '[]'::jsonb, decided_at = now(), decided_by_firebase_uid = ?
                WHERE id = ? AND tenant_id = ? AND status = 'PENDING'
                """,
                approverFirebaseUid,
                membershipId,
                societyId);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Membership request is no longer pending");
        }
        return getMembership(societyId, membershipId);
    }

    @Transactional
    public MembershipResponse assignRoles(UUID societyId, UUID membershipId, Set<Role> roles) {
        requireFirebaseClaimsSyncConfigured();
        tenantSql.setTenant(societyId);
        lockSociety(societyId);
        MembershipType membershipType = jdbcTemplate.query(
                        "SELECT membership_type FROM society_membership WHERE id = ? AND tenant_id = ? AND status = 'APPROVED'",
                        (rs, rowNum) -> MembershipType.valueOf(rs.getString(1)),
                        membershipId,
                        societyId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approved member was not found"));
        Set<Role> assignedRoles = validateRoles(membershipType, roles);
        preserveSecretary(societyId, membershipId, assignedRoles.contains(Role.SECRETARY));
        int updated = jdbcTemplate.update(
                """
                UPDATE society_membership
                SET roles = ?::jsonb, authorization_version = authorization_version + 1
                WHERE id = ? AND tenant_id = ? AND status = 'APPROVED'
                """,
                writeJson(assignedRoles),
                membershipId,
                societyId);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member roles could not be updated");
        }
        appendClaimsSync(societyId, membershipId);
        return getMembership(societyId, membershipId);
    }

    @Transactional
    public MembershipResponse setSuspended(UUID societyId, UUID membershipId, boolean suspended) {
        requireFirebaseClaimsSyncConfigured();
        tenantSql.setTenant(societyId);
        lockSociety(societyId);
        if (suspended) {
            preserveSecretary(societyId, membershipId, false);
        }
        String newStatus = suspended ? "SUSPENDED" : "APPROVED";
        int updated = jdbcTemplate.update(
                """
                UPDATE society_membership
                SET status = ?, authorization_version = authorization_version + 1
                WHERE id = ? AND tenant_id = ? AND status = ?
                """,
                newStatus,
                membershipId,
                societyId,
                suspended ? "APPROVED" : "SUSPENDED");
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member status could not be changed");
        }
        appendClaimsSync(societyId, membershipId);
        return getMembership(societyId, membershipId);
    }

    @Transactional(readOnly = true)
    public MembershipResponse getMembership(UUID societyId, UUID membershipId) {
        tenantSql.setTenant(societyId);
        return jdbcTemplate.queryForObject(
                """
                SELECT id, status, roles::text AS roles, authorization_version, claims_sync_version
                FROM society_membership WHERE tenant_id = ? AND id = ?
                """,
                this::mapMembership,
                societyId,
                membershipId);
    }

    private MembershipType requirePendingMembership(UUID societyId, UUID membershipId) {
        List<MembershipType> memberships = jdbcTemplate.query(
                "SELECT membership_type FROM society_membership WHERE tenant_id = ? AND id = ? AND status = 'PENDING' FOR UPDATE",
                (rs, rowNum) -> MembershipType.valueOf(rs.getString(1)),
                societyId,
                membershipId);
        if (memberships.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending membership request was not found");
        }
        return memberships.getFirst();
    }

    private Set<Role> validateRoles(MembershipType membershipType, Set<Role> roles) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(role -> !role.isAssignableToSocietyMembership())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more requested roles cannot be assigned");
        }
        if (membershipType == MembershipType.RESIDENT && roles.contains(Role.GUARD)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resident membership cannot receive the GUARD role");
        }
        if (membershipType == MembershipType.SECURITY_GUARD
                && !roles.equals(Set.of(Role.GUARD))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Security guard membership must have only the GUARD role");
        }
        return Set.copyOf(new LinkedHashSet<>(roles));
    }

    private void appendClaimsSync(UUID societyId, UUID membershipId) {
        JsonNode payload = jsonMapper.createObjectNode().put("membershipId", membershipId.toString());
        outboxWriter.append(new OutboxEvent(societyId, CLAIMS_SYNC_EVENT, payload));
    }

    private void requireFirebaseClaimsSyncConfigured() {
        if (!firebaseClaimsClient.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Firebase Admin credentials are required for membership changes");
        }
    }

    private void requirePhoneProtectionConfigured() {
        if (!phoneNumberProtector.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Phone contact encryption is not configured");
        }
    }

    private void preserveSecretary(UUID societyId, UUID membershipId, boolean remainsSecretary) {
        if (remainsSecretary) {
            return;
        }
        String secretaryRoleJson = writeJson(Set.of(Role.SECRETARY));
        Integer otherSecretaries = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM society_membership
                WHERE tenant_id = ? AND id <> ? AND status = 'APPROVED' AND roles @> ?::jsonb
                """,
                Integer.class,
                societyId,
                membershipId,
                secretaryRoleJson);
        Integer targetIsSecretary = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM society_membership
                WHERE tenant_id = ? AND id = ? AND status = 'APPROVED' AND roles @> ?::jsonb
                """,
                Integer.class,
                societyId,
                membershipId,
                secretaryRoleJson);
        if (targetIsSecretary != null && targetIsSecretary == 1 && (otherSecretaries == null || otherSecretaries == 0)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A society must retain at least one secretary");
        }
    }

    private void lockSociety(UUID societyId) {
        if (jdbcTemplate.queryForList("SELECT id FROM society WHERE id = ? FOR UPDATE", UUID.class, societyId)
                .isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Society was not found");
        }
    }

    private MembershipResponse mapMembership(ResultSet rs, int rowNum) throws SQLException {
        return new MembershipResponse(
                rs.getObject("id", UUID.class),
                MembershipStatus.valueOf(rs.getString("status")),
                readRoles(rs.getString("roles")),
                rs.getLong("authorization_version"),
                rs.getLong("claims_sync_version") >= rs.getLong("authorization_version"));
    }

    private Set<Role> readRoles(String json) {
        try {
            JsonNode roles = jsonMapper.readTree(json);
            Set<Role> values = new LinkedHashSet<>();
            roles.forEach(value -> values.add(Role.valueOf(value.asString())));
            return Set.copyOf(values);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new IllegalStateException("Stored membership roles are invalid", exception);
        }
    }

    private String writeJson(Set<Role> roles) {
        try {
            return jsonMapper.writeValueAsString(roles.stream().map(Role::name).sorted().toList());
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize membership roles", exception);
        }
    }

    private record ChangedMembership(UUID identityId, long authorizationVersion) {}
}
