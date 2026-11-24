package com.societix.platform.notification;

import com.societix.platform.outbox.OutboxEvent;
import com.societix.platform.outbox.OutboxWriter;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Public platform API for modules to enqueue a resident notification. */
@Service
public class NotificationService {

    public static final String EVENT_TYPE = "notification.requested";

    private final JdbcTemplate jdbcTemplate;
    private final OutboxWriter outboxWriter;
    private final JsonMapper jsonMapper;

    public NotificationService(JdbcTemplate jdbcTemplate, OutboxWriter outboxWriter, JsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.outboxWriter = outboxWriter;
        this.jsonMapper = jsonMapper;
    }

    /** Enqueues the request inside the caller's transaction, establishing RLS tenant context first. */
    @Transactional
    public UUID notifyResident(NotificationRequest request) {
        jdbcTemplate.queryForObject(
                "select set_config('app.tenant_id', ?, true)", String.class, request.societyId().toString());
        return outboxWriter.append(new OutboxEvent(
                request.societyId(), EVENT_TYPE, request.toPayload(jsonMapper)));
    }
}
