package com.societix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.societix.platform.outbox.OutboxEvent;
import com.societix.platform.outbox.OutboxWriter;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** SOC-20 outbox acceptance tests against the real PostgreSQL Testcontainer. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OutboxWriterIT {

    private final OutboxWriter outboxWriter;
    private final SocietyOnboardingService onboarding;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper objectMapper;
    private final TransactionTemplate transactions;

    @Autowired
    OutboxWriterIT(
            OutboxWriter outboxWriter,
            SocietyOnboardingService onboarding,
            JdbcTemplate jdbcTemplate,
            JsonMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.outboxWriter = outboxWriter;
        this.onboarding = onboarding;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Test
    void eventCanBeWrittenInsideBusinessTransactionAndCarriesSocietyId() {
        Society society = createSociety();
        JsonNode payload = objectMapper.createObjectNode().put("invoiceId", "inv-123").put("amountPaise", 2500);

        UUID eventId = inTenantTransaction(society.getId(), () -> outboxWriter.append(
                new OutboxEvent(society.getId(), "billing.invoice.raised", payload)));

        assertThat(eventId).isNotNull();
        assertThat(countAsTenant(society.getId(), "select count(*) from outbox where id = '" + eventId + "'"))
                .isEqualTo(1);
        var stored = inTenantTransaction(
                society.getId(),
                () -> jdbcTemplate.queryForMap(
                        "select tenant_id, event_type, payload::text as payload, processed_at, attempts "
                                + "from outbox where id = ?",
                        eventId));
        assertThat(stored.get("tenant_id")).isEqualTo(society.getId());
        assertThat(stored.get("event_type")).isEqualTo("billing.invoice.raised");
        assertThat(readJson((String) stored.get("payload"))).isEqualTo(payload);
        assertThat(stored.get("processed_at")).isNull();
        assertThat(stored.get("attempts")).isEqualTo(0);
    }

    @Test
    void societyRlsHidesOutboxEventsFromOtherAndMissingTenants() {
        Society societyA = createSociety();
        Society societyB = createSociety();
        UUID eventId = inTenantTransaction(societyA.getId(), () -> outboxWriter.append(new OutboxEvent(
                societyA.getId(), "payment.settled", objectMapper.createObjectNode().put("paymentId", "p-456"))));

        assertThat(countAsTenant(societyA.getId(), "select count(*) from outbox where id = '" + eventId + "'"))
                .isEqualTo(1);
        assertThat(countAsTenant(societyB.getId(), "select count(*) from outbox where id = '" + eventId + "'"))
                .isZero();
        assertThat(countAsTenant(null, "select count(*) from outbox")).isZero();
        assertThat(countAsTenant("not-a-uuid", "select count(*) from outbox")).isZero();
    }

    @Test
    void rowLevelSecurityRejectsWritingForAnotherSociety() {
        Society societyA = createSociety();
        Society societyB = createSociety();
        OutboxEvent event = new OutboxEvent(
                societyB.getId(), "payment.settled", objectMapper.createObjectNode().put("paymentId", "p-789"));

        assertThatThrownBy(() -> inTenantTransaction(societyA.getId(), () -> outboxWriter.append(event)))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasMessageContaining("row-level security policy");
        assertThat(countAsTenant(societyB.getId(), "select count(*) from outbox")).isZero();
    }

    @Test
    void outboxWriteRollsBackWithTheCallerTransaction() {
        Society society = createSociety();
        JsonNode payload = objectMapper.createObjectNode().put("reason", "rollback check");

        assertThatThrownBy(() -> inTenantTransaction(society.getId(), () -> {
                    outboxWriter.append(new OutboxEvent(society.getId(), "society.updated", payload));
                    throw new IntentionalRollbackException();
                }))
                .isInstanceOf(IntentionalRollbackException.class);

        assertThat(countAsTenant(society.getId(), "select count(*) from outbox where event_type = 'society.updated'"))
                .isZero();
    }

    @Test
    void writerRequiresAnExistingTransaction() {
        Society society = createSociety();
        OutboxEvent event = new OutboxEvent(
                society.getId(), "test.event", objectMapper.createObjectNode().put("key", "value"));

        assertThatThrownBy(() -> outboxWriter.append(event))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    private Society createSociety() {
        return onboarding.onboard(new CreateSocietyRequest(
                "Outbox Test Society " + UUID.randomUUID(), "1 Test Road, Pune", "MH"));
    }

    private <T> T inTenantTransaction(UUID societyId, java.util.function.Supplier<T> work) {
        return transactions.execute(status -> {
            jdbcTemplate.queryForObject(
                    "select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
            return work.get();
        });
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

    private int countAsTenant(Object societyId, String sql) {
        return inTenantTransaction(societyId, () -> jdbcTemplate.queryForObject(sql, Integer.class));
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new AssertionError("PostgreSQL returned invalid JSON", exception);
        }
    }

    private static final class IntentionalRollbackException extends RuntimeException {}
}
