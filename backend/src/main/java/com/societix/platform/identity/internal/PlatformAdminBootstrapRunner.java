package com.societix.platform.identity.internal;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.societix.platform.identity.domain.PhoneNumberProtector;
import com.societix.platform.identity.domain.Role;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** One-time operator bootstrap, activated only by an explicit Firebase UID environment property. */
@Component
@ConditionalOnExpression(
        "('${societix.security.firebase.admin.enabled:false}' == 'true' or "
                + "'${societix.security.firebase.auth-emulator-host:}' != '') and "
                + "'${societix.security.platform-admin.bootstrap.firebase-uid:}' != ''")
class PlatformAdminBootstrapRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PlatformAdminBootstrapRunner.class);

    private final FirebaseAuth firebaseAuth;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final PhoneNumberProtector phoneNumberProtector;
    private final String firebaseUid;

    PlatformAdminBootstrapRunner(
            FirebaseAuth firebaseAuth,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            PhoneNumberProtector phoneNumberProtector,
            @Value("${societix.security.platform-admin.bootstrap.firebase-uid}") String firebaseUid) {
        this.firebaseAuth = firebaseAuth;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.phoneNumberProtector = phoneNumberProtector;
        this.firebaseUid = firebaseUid;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (firebaseUid.isBlank()) {
            throw new IllegalStateException("Platform admin bootstrap UID cannot be blank");
        }
        UserRecord firebaseUser = loadFirebaseUser();
        if (firebaseUser.isDisabled() || firebaseUser.getPhoneNumber() == null) {
            throw new IllegalStateException("Bootstrap Firebase user must be enabled and have a verified phone number");
        }
        byte[] protectedPhone = phoneNumberProtector.encrypt(firebaseUser.getPhoneNumber());
        PlatformAdminIdentity admin = transactionTemplate.execute(status -> findOrCreatePlatformAdmin(protectedPhone));
        if (admin == null) {
            throw new IllegalStateException("Platform admin bootstrap did not return a user ID");
        }

        try {
            firebaseAuth.setCustomUserClaims(firebaseUid, Map.of(
                    "userId", admin.userId().toString(),
                    "roles", List.of(Role.PLATFORM_ADMIN.name()),
                    "authorizationVersion", admin.authorizationVersion()));
        } catch (FirebaseAuthException exception) {
            throw new IllegalStateException("Could not set initial platform administrator claims", exception);
        }
        logger.info("Initial platform administrator bootstrap completed");
    }

    private UserRecord loadFirebaseUser() {
        try {
            return firebaseAuth.getUser(firebaseUid);
        } catch (FirebaseAuthException exception) {
            throw new IllegalStateException("Bootstrap Firebase UID was not found", exception);
        }
    }

    private PlatformAdminIdentity findOrCreatePlatformAdmin(byte[] protectedPhone) {
        List<PlatformAdminIdentity> existing = jdbcTemplate.query(
                """
                SELECT pa.identity_id, pa.authorization_version, pa.status FROM platform_admin_account pa
                JOIN identity_account ia ON ia.id = pa.identity_id
                WHERE ia.firebase_uid = ?
                """,
                (rs, rowNum) -> new PlatformAdminIdentity(
                        rs.getObject("identity_id", UUID.class),
                        rs.getLong("authorization_version"),
                        rs.getString("status")),
                firebaseUid);
        if (!existing.isEmpty()) {
            PlatformAdminIdentity identity = existing.getFirst();
            if (!"ACTIVE".equals(identity.status())) {
                throw new IllegalStateException("The existing platform administrator is not active");
            }
            return identity;
        }
        Integer existingAdmins = jdbcTemplate.queryForObject("SELECT count(*) FROM platform_admin_account", Integer.class);
        if (existingAdmins != null && existingAdmins > 0) {
            throw new IllegalStateException("A platform administrator is already provisioned for another Firebase UID");
        }

        UUID userId = UUID.randomUUID();
        try {
            userId = jdbcTemplate.queryForObject(
                    """
                    INSERT INTO identity_account (id, firebase_uid, phone_number_cipher) VALUES (?, ?, ?)
                    ON CONFLICT (firebase_uid) DO UPDATE SET phone_number_cipher = EXCLUDED.phone_number_cipher
                    RETURNING id
                    """,
                    UUID.class,
                    userId,
                    firebaseUid,
                    protectedPhone);
            jdbcTemplate.update("INSERT INTO platform_admin_account (identity_id) VALUES (?)", userId);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Could not create initial platform administrator", exception);
        }
        return new PlatformAdminIdentity(userId, 1L, "ACTIVE");
    }

    private record PlatformAdminIdentity(UUID userId, long authorizationVersion, String status) {}
}
