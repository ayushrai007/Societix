package com.societix.platform.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.societix.TestcontainersConfiguration;
import com.societix.platform.society.api.CreateSocietyRequest;
import com.societix.platform.society.internal.SocietyOnboardingService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MembershipAuthorizationServiceIT {

    private final MembershipAuthorizationService authorization;
    private final JdbcTemplate jdbcTemplate;
    private final SocietyOnboardingService societies;
    private final TransactionTemplate transactions;

    @Autowired
    MembershipAuthorizationServiceIT(
            MembershipAuthorizationService authorization,
            JdbcTemplate jdbcTemplate,
            SocietyOnboardingService societies,
            PlatformTransactionManager transactionManager) {
        this.authorization = authorization;
        this.jdbcTemplate = jdbcTemplate;
        this.societies = societies;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Test
    void platformAdminRequiresActiveAccountAndMatchingAuthorizationVersion() {
        UUID userId = UUID.randomUUID();
        String firebaseUid = "platform-admin-" + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity_account (id, firebase_uid, phone_number_cipher) VALUES (?, ?, ?)",
                userId,
                firebaseUid,
                new byte[] {1});
        jdbcTemplate.update(
                "INSERT INTO platform_admin_account (identity_id, status, authorization_version) VALUES (?, 'ACTIVE', 3)",
                userId);

        assertThat(authorization.hasActivePlatformAdmin(firebaseUid, userId, 3)).isTrue();
        assertThat(authorization.hasActivePlatformAdmin(firebaseUid, userId, 2)).isFalse();

        jdbcTemplate.update("UPDATE platform_admin_account SET status = 'SUSPENDED' WHERE identity_id = ?", userId);
        assertThat(authorization.hasActivePlatformAdmin(firebaseUid, userId, 3)).isFalse();
    }

    @Test
    void pendingSocietyMembershipCannotAuthorizeProtectedRequests() {
        UUID societyId = societies.onboard(new CreateSocietyRequest("Pending Test Society", "1 Test Road", "MH"))
                .getId();
        UUID userId = UUID.randomUUID();
        String firebaseUid = "pending-user-" + UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity_account (id, firebase_uid, phone_number_cipher) VALUES (?, ?, ?)",
                userId,
                firebaseUid,
                new byte[] {1});

        transactions.executeWithoutResult(status -> {
            jdbcTemplate.queryForObject(
                    "select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
            jdbcTemplate.update(
                    "INSERT INTO society_membership (id, tenant_id, identity_id, membership_type, status) "
                            + "VALUES (?, ?, ?, 'RESIDENT', 'PENDING')",
                    membershipId,
                    societyId,
                    userId);
        });

        assertThat(authorization.hasCurrentApprovedMembership(firebaseUid, userId, societyId, 0)).isFalse();
    }
}
