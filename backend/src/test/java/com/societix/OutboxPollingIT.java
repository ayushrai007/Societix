package com.societix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.societix.platform.outbox.OutboxEvent;
import com.societix.platform.outbox.OutboxWriter;
import com.societix.platform.outbox.internal.OutboxPoller;
import com.societix.platform.society.api.CreateSocietyRequest;
import com.societix.platform.society.domain.Society;
import com.societix.platform.society.internal.SocietyOnboardingService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * SOC-21 integration tests against the PostgreSQL Testcontainer.
 *
 * <ul>
 *   <li>"unprocessed events are dispatched within seconds" — {@link #unprocessedEventsAreDispatchedWithinSeconds()}</li>
 *   <li>"only one instance processes at a time" — {@link #shedLockAllowsOnlyOneInstanceToAcquireTheDrainLock()}</li>
 *   <li>Retry after handler failure — {@link #failedHandlerIsRetriedAfterBackoff()}</li>
 * </ul>
 */
@SpringBootTest(properties = {
    "societix.outbox.poll-delay-ms=200",
    "societix.outbox.batch-size=50"
})
@Import({TestcontainersConfiguration.class, OutboxTestHandlerConfiguration.class})
class OutboxPollingIT {

    private final OutboxWriter writer;
    private final SocietyOnboardingService onboarding;
    private final OutboxTestHandlerConfiguration.RecordingOutboxEventHandler handler;
    private final LockProvider lockProvider;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactions;
    private final JsonMapper objectMapper;

    @Autowired
    OutboxPollingIT(
            OutboxWriter writer,
            SocietyOnboardingService onboarding,
            OutboxTestHandlerConfiguration.RecordingOutboxEventHandler handler,
            LockProvider lockProvider,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            JsonMapper objectMapper) {
        this.writer = writer;
        this.onboarding = onboarding;
        this.handler = handler;
        this.lockProvider = lockProvider;
        this.jdbcTemplate = jdbcTemplate;
        this.transactions = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
    }

    @Test
    void unprocessedEventsAreDispatchedWithinSeconds() {
        Society society = createSociety();
        UUID eventId = appendEvent(society.getId());

        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            assertThat(handler.effectCount(eventId)).isEqualTo(1);
            assertThat(isProcessed(society.getId(), eventId)).isTrue();
        });
    }

    @Test
    void failedHandlerIsRetriedAfterBackoff() {
        Society society = createSociety();
        handler.failNextDelivery();
        UUID eventId = appendEvent(society.getId());

        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            assertThat(handler.effectCount(eventId)).isEqualTo(1);
            assertThat(isProcessed(society.getId(), eventId)).isTrue();
            assertThat(attempts(society.getId(), eventId)).isEqualTo(1);
        });
    }

    @Test
    void shedLockAllowsOnlyOneInstanceToAcquireTheDrainLock() throws NoSuchMethodException {
        SchedulerLock annotation = OutboxPoller.class.getMethod("poll").getAnnotation(SchedulerLock.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("societixOutboxDrain");
        LockConfiguration lock = new LockConfiguration(
                Instant.now(), "societixOutboxDrain-integration-test", Duration.ofMinutes(1), Duration.ZERO);

        SimpleLock first = lockProvider.lock(lock).orElseThrow();
        try {
            assertThat(lockProvider.lock(lock)).isEmpty();
        } finally {
            first.unlock();
        }
        SimpleLock next = lockProvider.lock(lock).orElseThrow();
        next.unlock();
    }

    private Society createSociety() {
        return onboarding.onboard(new CreateSocietyRequest(
                "Outbox Poller Test " + UUID.randomUUID(), "2 Poller Road, Pune", "MH"));
    }

    private UUID appendEvent(UUID societyId) {
        return transactions.execute(status -> {
            setTenant(societyId);
            return writer.append(new OutboxEvent(
                    societyId,
                    "societix.test.idempotent",
                    objectMapper.createObjectNode().put("test", true)));
        });
    }

    private boolean isProcessed(UUID societyId, UUID eventId) {
        return transactions.execute(status -> {
            setTenant(societyId);
            return jdbcTemplate.queryForObject(
                    "select processed_at is not null from outbox where tenant_id = ? and id = ?",
                    Boolean.class,
                    societyId,
                    eventId);
        });
    }

    private int attempts(UUID societyId, UUID eventId) {
        return transactions.execute(status -> {
            setTenant(societyId);
            return jdbcTemplate.queryForObject(
                    "select attempts from outbox where tenant_id = ? and id = ?", Integer.class, societyId, eventId);
        });
    }

    private void setTenant(UUID societyId) {
        jdbcTemplate.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
    }
}
