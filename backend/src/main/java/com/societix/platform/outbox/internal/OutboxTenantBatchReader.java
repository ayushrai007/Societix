package com.societix.platform.outbox.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Reads pending IDs only after establishing the tenant context required by outbox RLS. */
@Component
public class OutboxTenantBatchReader {

    private final JdbcTemplate jdbcTemplate;

    public OutboxTenantBatchReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<UUID> findDueEventIds(UUID societyId, int limit) {
        jdbcTemplate.queryForObject(
                "select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
        return jdbcTemplate.queryForList(
                """
                SELECT id
                FROM outbox
                WHERE tenant_id = ? AND processed_at IS NULL AND next_attempt_at <= now()
                ORDER BY created_at, id
                LIMIT ?
                """,
                UUID.class,
                societyId,
                limit);
    }
}
