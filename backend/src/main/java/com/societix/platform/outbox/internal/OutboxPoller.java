package com.societix.platform.outbox.internal;

import java.util.List;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically drains due outbox rows, processing work under each event's society RLS context. */
@Component
@ConditionalOnProperty(prefix = "societix.outbox.poller", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPoller.class);
    private static final int MAX_EVENTS_PER_SOCIETY_PER_RUN = 10;

    private final JdbcTemplate jdbcTemplate;
    private final OutboxTenantBatchReader tenantBatchReader;
    private final OutboxEventProcessor eventProcessor;
    private final OutboxFailureRecorder failureRecorder;
    private final int batchSize;

    public OutboxPoller(
            JdbcTemplate jdbcTemplate,
            OutboxTenantBatchReader tenantBatchReader,
            OutboxEventProcessor eventProcessor,
            OutboxFailureRecorder failureRecorder,
            @org.springframework.beans.factory.annotation.Value("${societix.outbox.batch-size:100}") int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("societix.outbox.batch-size must be positive");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.tenantBatchReader = tenantBatchReader;
        this.eventProcessor = eventProcessor;
        this.failureRecorder = failureRecorder;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${societix.outbox.poll-delay-ms:3000}")
    @SchedulerLock(name = "societixOutboxDrain", lockAtMostFor = "PT5M")
    public void poll() {
        LockAssert.assertLocked();
        List<UUID> societies = jdbcTemplate.queryForList(
                "SELECT tenant_id FROM outbox_pending_tenants(?)", UUID.class, batchSize);
        int remaining = batchSize;
        for (UUID societyId : societies) {
            if (remaining <= 0) {
                break;
            }
            int societyLimit = Math.min(remaining, MAX_EVENTS_PER_SOCIETY_PER_RUN);
            List<UUID> eventIds = tenantBatchReader.findDueEventIds(societyId, societyLimit);
            for (UUID eventId : eventIds) {
                try {
                    eventProcessor.process(societyId, eventId);
                } catch (RuntimeException exception) {
                    failureRecorder.recordFailure(societyId, eventId);
                    logger.atWarn()
                            .addKeyValue("eventId", eventId)
                            .addKeyValue("societyId", societyId)
                            .addKeyValue("failureType", exception.getClass().getSimpleName())
                            .log("Outbox event dispatch failed; retry was scheduled");
                }
                remaining--;
            }
        }
    }
}
