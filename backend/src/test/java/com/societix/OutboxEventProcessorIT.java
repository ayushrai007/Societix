package com.societix;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * SOC-21 mandatory idempotency test against real PostgreSQL.
 *
 * <p>"a repeated call/event causes no duplicate effect" — {@link #repeatedDeliveryHasNoDuplicateHandlerEffect()}.
 */
@SpringBootTest(properties = "societix.outbox.poller.enabled=false")
@Import({TestcontainersConfiguration.class, OutboxTestHandlerConfiguration.class})
class OutboxEventProcessorIT {

    private final OutboxWriter writer;
    private final OutboxEventProcessor processor;
    private final SocietyOnboardingService onboarding;
    private final OutboxTestHandlerConfiguration.RecordingOutboxEventHandler handler;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactions;
    private final JsonMapper objectMapper;

    @Autowired
    OutboxEventProcessorIT(
            OutboxWriter writer,
            OutboxEventProcessor processor,
            SocietyOnboardingService onboarding,
            OutboxTestHandlerConfiguration.RecordingOutboxEventHandler handler,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            JsonMapper objectMapper) {
        this.writer = writer;
        this.processor = processor;
        this.onboarding = onboarding;
        this.handler = handler;
        this.jdbcTemplate = jdbcTemplate;
        this.transactions = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
    }

    @Test
    void repeatedDeliveryHasNoDuplicateHandlerEffect() {
        Society society = onboarding.onboard(new CreateSocietyRequest(
                "Outbox Idempotency Test " + UUID.randomUUID(), "3 Idempotency Road, Pune", "MH"));
        UUID eventId = transactions.execute(status -> {
            setTenant(society.getId());
            return writer.append(new OutboxEvent(
                    society.getId(),
                    "societix.test.idempotent",
                    objectMapper.createObjectNode().put("test", "replay")));
        });

        assertThat(processor.process(society.getId(), eventId)).isTrue();
        resetProcessedAt(society.getId(), eventId); // Simulate a redelivery after a dispatcher restart.
        assertThat(processor.process(society.getId(), eventId)).isTrue();
        assertThat(processor.process(society.getId(), eventId)).isFalse();

        assertThat(handler.effectCount(eventId)).isEqualTo(1);
        assertThat(countHandlerReceipts(society.getId(), eventId)).isEqualTo(1);
    }

    private int countHandlerReceipts(UUID societyId, UUID eventId) {
        return transactions.execute(status -> {
            setTenant(societyId);
            return jdbcTemplate.queryForObject(
                    "select count(*) from outbox_handler_receipt where tenant_id = ? and event_id = ?",
                    Integer.class,
                    societyId,
                    eventId);
        });
    }

    private void resetProcessedAt(UUID societyId, UUID eventId) {
        transactions.executeWithoutResult(status -> {
            setTenant(societyId);
            jdbcTemplate.update(
                    "update outbox set processed_at = null where tenant_id = ? and id = ?", societyId, eventId);
        });
    }

    private void setTenant(UUID societyId) {
        jdbcTemplate.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
    }
}
