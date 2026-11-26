package com.societix.platform.identity.internal;

import com.societix.platform.identity.domain.MembershipStatus;
import com.societix.platform.identity.domain.SocietixRoleClaims;
import com.societix.platform.outbox.OutboxEventHandler;
import com.societix.platform.outbox.OutboxMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Synchronizes current membership claims; the database row wins over possibly older outbox payloads. */
@Component
public class FirebaseClaimsSyncHandler implements OutboxEventHandler {

    private static final String HANDLER_KEY = "platform.firebase-membership-claims";

    private final JdbcTemplate jdbcTemplate;
    private final FirebaseClaimsClient firebaseClaims;
    private final JsonMapper jsonMapper;

    public FirebaseClaimsSyncHandler(
            JdbcTemplate jdbcTemplate, FirebaseClaimsClient firebaseClaims, JsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.firebaseClaims = firebaseClaims;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String eventType() {
        return MembershipService.CLAIMS_SYNC_EVENT;
    }

    @Override
    public String handlerKey() {
        return HANDLER_KEY;
    }

    @Override
    @Transactional
    public void handle(OutboxMessage message) {
        UUID membershipId = UUID.fromString(message.payload().get("membershipId").asString());
        List<MembershipClaims> current = jdbcTemplate.query(
                """
                SELECT sm.identity_id, sm.status, sm.roles::text AS roles, sm.authorization_version,
                       ia.firebase_uid, pa.status AS platform_admin_status,
                       pa.authorization_version AS platform_admin_version
                FROM society_membership sm
                JOIN identity_account ia ON ia.id = sm.identity_id
                LEFT JOIN platform_admin_account pa ON pa.identity_id = ia.id
                WHERE sm.tenant_id = ? AND sm.id = ?
                FOR UPDATE OF sm
                """,
                (rs, rowNum) -> new MembershipClaims(
                        rs.getObject("identity_id", UUID.class),
                        MembershipStatus.valueOf(rs.getString("status")),
                        rs.getString("roles"),
                        rs.getLong("authorization_version"),
                        rs.getString("firebase_uid"),
                        rs.getString("platform_admin_status"),
                        rs.getObject("platform_admin_version", Long.class)),
                message.societyId(),
                membershipId);
        if (current.isEmpty()) {
            throw new IllegalStateException("Membership for claim synchronization was not found");
        }

        MembershipClaims membership = current.getFirst();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", membership.identityId().toString());
        List<String> roles = new ArrayList<>();
        boolean approvedMembership = membership.status() == MembershipStatus.APPROVED;
        if (approvedMembership) {
            claims.put("tenantId", message.societyId().toString());
            claims.put("membershipAuthorizationVersion", membership.authorizationVersion());
            roles.addAll(readRoles(membership.rolesJson()));
        }
        boolean activePlatformAdmin = "ACTIVE".equals(membership.platformAdminStatus());
        if (activePlatformAdmin) {
            roles.add(SocietixRoleClaims.PLATFORM_ADMIN);
        }
        claims.put("roles", List.copyOf(roles));
        claims.put(
                "authorizationVersion",
                activePlatformAdmin ? membership.platformAdminVersion() : membership.authorizationVersion());
        firebaseClaims.setCustomClaims(membership.firebaseUid(), Map.copyOf(claims));

        jdbcTemplate.update(
                """
                UPDATE society_membership SET claims_sync_version = ?
                WHERE tenant_id = ? AND id = ? AND authorization_version = ?
                """,
                membership.authorizationVersion(),
                message.societyId(),
                membershipId,
                membership.authorizationVersion());
    }

    private List<String> readRoles(String json) {
        try {
            JsonNode rolesNode = jsonMapper.readTree(json);
            List<String> roles = new ArrayList<>();
            rolesNode.forEach(role -> roles.add(role.asString()));
            return List.copyOf(roles);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored membership roles are invalid", exception);
        }
    }

    private record MembershipClaims(
            UUID identityId,
            MembershipStatus status,
            String rolesJson,
            long authorizationVersion,
            String firebaseUid,
            String platformAdminStatus,
            Long platformAdminVersion) {}
}
