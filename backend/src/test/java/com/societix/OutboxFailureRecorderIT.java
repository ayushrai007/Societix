package com.societix;

import static org.assertj.core.api.Assertions.assertThat;

import com.societix.platform.outbox.OutboxEvent;
import com.societix.platform.outbox.OutboxWriter;
import com.societix.platform.outbox.internal.OutboxFailureRecorder;
import com.societix.platform.society.api.CreateSocietyRequest;
import com.societix.platform.society.domain.Society;
import com.societix.platform.society.internal.SocietyOnboardingService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Retry exhaustion behavior against the PostgreSQL Testcontainer. */
@SpringBootTest(properties = "societix.outbox.poller.enabled=false")
@Import(TestcontainersConfiguration.class)
class OutboxFailureRecorderIT {

    private final OutboxWriter writer;
    private final OutboxFailureRecorder failureRecorder;
    private final SocietyOnboardingService onboarding;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper objectMapper;
    private final TransactionTemplate transactions;

    @Autowired
    OutboxFailureRecorderIT(
            OutboxWriter writer,
            OutboxFailureRecorder failureRecorder,
            SocietyOnboardingService onboarding,
            JdbcTemplate jdbcTemplate,
            JsonMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.writer = writer;
        this.failureRecorder = failureRecorder;
        this.onboarding = onboarding;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Test
    void parksEventAfterTenFailedAttempts() {
        Society society = onboarding.onboard(new CreateSocietyRequest(
                "Outbox Retry Test " + UUID.randomUUID(), "5 Retry Road, Pune", "MH"));
        UUID eventId = transactions.execute(status -> {
            setTenant(society.getId());
            return writer.append(new OutboxEvent(
                    society.getId(), "societix.test.retry", objectMapper.createObjectNode().put("test", true)));
        });

        for (int attempt = 0; attempt < 10; attempt++) {
            transactions.executeWithoutResult(status -> {
                setTenant(society.getId());
                failureRecorder.recordFailure(society.getId(), eventId);
            });
        }

        transactions.executeWithoutResult(status -> {
            setTenant(society.getId());
            var row = jdbcTemplate.queryForMap(
                    "select attempts, failed_at from outbox where tenant_id = ? and id = ?",
                    society.getId(),
                    eventId);
            assertThat(row.get("attempts")).isEqualTo(10);
            assertThat(row.get("failed_at")).isNotNull();
            assertThat(jdbcTemplate.queryForObject(
                            "select count(*) from outbox_pending_tenants(100) where tenant_id = ?",
                            Integer.class,
                            society.getId()))
                    .isZero();
        });
    }

    private void setTenant(UUID societyId) {
        jdbcTemplate.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
    }
}
