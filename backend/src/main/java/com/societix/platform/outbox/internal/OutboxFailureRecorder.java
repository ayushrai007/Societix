package com.societix.platform.outbox.internal;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Records retry attempts with bounded exponential backoff, in a transaction separate from dispatch. */
@Component
public class OutboxFailureRecorder {

    private final JdbcTemplate jdbcTemplate;

    public OutboxFailureRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void recordFailure(UUID societyId, UUID eventId) {
        jdbcTemplate.queryForObject(
                "select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
        jdbcTemplate.update(
                """
                UPDATE outbox
                SET attempts = attempts + 1,
                    next_attempt_at = now() + (LEAST(256, 1 << LEAST(attempts, 8)) * interval '1 second')
                WHERE id = ? AND tenant_id = ? AND processed_at IS NULL
                """,
                eventId,
                societyId);
    }
}
