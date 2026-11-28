package com.societix.platform.identity.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.societix.platform.identity.domain.PhoneNumberProtector;
import com.societix.platform.identity.domain.Role;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class PlatformAdminBootstrapRunnerTest {

    private static final String FIREBASE_UID = "bootstrap-admin-uid";
    private static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void bootstrapsOnlyTheExplicitVerifiedFirebaseIdentityAndSetsAdminClaims() throws Exception {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        UserRecord firebaseUser = mock(UserRecord.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        TransactionTemplate transaction = mock(TransactionTemplate.class);
        UUID userId = UUID.randomUUID();
        when(firebaseAuth.getUser(FIREBASE_UID)).thenReturn(firebaseUser);
        when(firebaseUser.isDisabled()).thenReturn(false);
        when(firebaseUser.getPhoneNumber()).thenReturn("+919876543210");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        when(jdbc.queryForObject(anyString(), eq(UUID.class), any(), any(), any())).thenReturn(userId);
        doAnswer(invocation -> {
                    TransactionCallback callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                })
                .when(transaction)
                .execute(isA(TransactionCallback.class));

        PlatformAdminBootstrapRunner runner = new PlatformAdminBootstrapRunner(
                firebaseAuth, jdbc, transaction, new PhoneNumberProtector(TEST_KEY), FIREBASE_UID);
        runner.run(new DefaultApplicationArguments());

        verify(jdbc).update(anyString(), eq(userId));
        verify(firebaseAuth).setCustomUserClaims(eq(FIREBASE_UID), eq(Map.of(
                "userId", userId.toString(),
                "roles", List.of(Role.PLATFORM_ADMIN.name()),
                "authorizationVersion", 1L)));
    }
}
