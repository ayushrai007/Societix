package com.societix.platform.identity.internal;

import com.societix.platform.identity.domain.TenantSql;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves signed token identity and version against current membership/operator state. */
@Service
public class MembershipAuthorizationService {

    private final JdbcTemplate jdbcTemplate;
    private final TenantSql tenantSql;

    public MembershipAuthorizationService(JdbcTemplate jdbcTemplate, TenantSql tenantSql) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantSql = tenantSql;
    }

    @Transactional(readOnly = true)
    public boolean hasCurrentApprovedMembership(
            String firebaseUid, UUID userId, UUID societyId, long tokenAuthorizationVersion) {
        tenantSql.setTenant(societyId);
        Integer matchingMemberships = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM society_membership sm
                JOIN identity_account ia ON ia.id = sm.identity_id
                WHERE sm.tenant_id = ? AND sm.identity_id = ? AND ia.firebase_uid = ?
                  AND sm.status = 'APPROVED' AND sm.authorization_version = ?
                """,
                Integer.class,
                societyId,
                userId,
                firebaseUid,
                tokenAuthorizationVersion);
        return matchingMemberships != null && matchingMemberships == 1;
    }

    @Transactional(readOnly = true)
    public boolean hasActivePlatformAdmin(String firebaseUid, UUID userId, long tokenAuthorizationVersion) {
        Integer matches = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM platform_admin_account pa
                JOIN identity_account ia ON ia.id = pa.identity_id
                WHERE pa.identity_id = ? AND ia.firebase_uid = ? AND pa.status = 'ACTIVE'
                  AND pa.authorization_version = ?
                """,
                Integer.class,
                userId,
                firebaseUid,
                tokenAuthorizationVersion);
        return matches != null && matches == 1;
    }
}
