package com.societix.platform.outbox;

import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes domain events to PostgreSQL in the caller's business transaction. */
@Component
public class OutboxWriter {

    private final JdbcTemplate jdbcTemplate;

    public OutboxWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Appends an event using the active transaction and returns its generated identifier.
     * Requiring an existing transaction prevents accidental out-of-band event writes.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID append(OutboxEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        UUID eventId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO outbox (id, tenant_id, event_type, payload)
                VALUES (?, ?, ?, ?::jsonb)
                """,
                eventId,
                event.societyId(),
                event.eventType(),
                event.payload().toString());
        return eventId;
    }
}
