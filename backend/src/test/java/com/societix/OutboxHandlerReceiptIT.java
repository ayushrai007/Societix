package com.societix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.societix.platform.outbox.OutboxEvent;
import com.societix.platform.outbox.OutboxWriter;
import com.societix.platform.outbox.internal.OutboxEventProcessor;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** RLS isolation tests for handler receipts against the PostgreSQL Testcontainer. */
@SpringBootTest(properties = "societix.outbox.poller.enabled=false")
@Import(TestcontainersConfiguration.class)
class OutboxHandlerReceiptIT {

    private final OutboxWriter writer;
    private final OutboxEventProcessor processor;
    private final SocietyOnboardingService onboarding;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper objectMapper;
    private final TransactionTemplate transactions;

    @Autowired
    OutboxHandlerReceiptIT(
            OutboxWriter writer,
            OutboxEventProcessor processor,
            SocietyOnboardingService onboarding,
            JdbcTemplate jdbcTemplate,
            JsonMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.writer = writer;
        this.processor = processor;
        this.onboarding = onboarding;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Test
    void receiptRowsAreIsolatedAndCrossTenantWritesAreRejected() {
        Society societyA = createSociety();
        Society societyB = createSociety();
        UUID eventA = appendEvent(societyA.getId());
        UUID eventB = appendEvent(societyB.getId());
        String handlerKey = "receipt-it-handler";
        inTenantTransaction(societyA.getId(), () -> jdbcTemplate.update(
                "insert into outbox_handler_receipt (tenant_id, event_id, handler_key) values (?, ?, ?)",
                societyA.getId(),
                eventA,
                handlerKey));

        assertThat(countAsTenant(societyA.getId(), eventA)).isEqualTo(1);
        assertThat(countAsTenant(societyB.getId(), eventA)).isZero();
        assertThat(countAsTenant(null, eventA)).isZero();
        assertThat(countAsTenant("not-a-uuid", eventA)).isZero();
        assertThat(processor.process(societyB.getId(), eventA)).isFalse();

        assertThatThrownBy(() -> inTenantTransaction(societyA.getId(), () -> jdbcTemplate.update(
                        "insert into outbox_handler_receipt (tenant_id, event_id, handler_key) values (?, ?, ?)",
                        societyB.getId(),
                        eventB,
                        "cross-tenant-handler")))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .rootCause()
                .hasMessageContaining("row-level security policy");
        assertThat(countAsTenant(societyB.getId(), eventB)).isZero();
    }

    private Society createSociety() {
        return onboarding.onboard(new CreateSocietyRequest(
                "Outbox Receipt Test " + UUID.randomUUID(), "4 Receipt Road, Pune", "MH"));
    }

    private UUID appendEvent(UUID societyId) {
        return inTenantTransaction(societyId, () -> writer.append(new OutboxEvent(
                societyId, "societix.test.receipt", objectMapper.createObjectNode().put("test", true))));
    }

    private int countAsTenant(Object societyId, UUID eventId) {
        return inTenantTransaction(societyId, () -> jdbcTemplate.queryForObject(
                "select count(*) from outbox_handler_receipt where event_id = ?", Integer.class, eventId));
    }

    private <T> T inTenantTransaction(Object societyId, java.util.function.Supplier<T> work) {
        return transactions.execute(status -> {
            jdbcTemplate.queryForObject(
                    "select set_config('app.tenant_id', ?, true)",
                    String.class,
                    societyId == null ? "" : societyId.toString());
            return work.get();
        });
    }
}
