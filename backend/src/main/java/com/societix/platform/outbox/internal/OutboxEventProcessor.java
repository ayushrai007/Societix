package com.societix.platform.outbox.internal;

import com.societix.platform.outbox.OutboxEventHandler;
import com.societix.platform.outbox.OutboxMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Runs each event and its database-backed idempotency receipts in one tenant transaction. */
@Component
public class OutboxEventProcessor {

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper objectMapper;
    private final Map<String, List<OutboxEventHandler>> handlersByEventType;

    public OutboxEventProcessor(
            JdbcTemplate jdbcTemplate, JsonMapper objectMapper, List<OutboxEventHandler> handlers) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.handlersByEventType = indexHandlers(handlers);
    }

    /** Returns false if the row is already processed, missing, or not due yet. */
    @Transactional
    public boolean process(UUID societyId, UUID eventId) {
        jdbcTemplate.queryForObject(
                "select set_config('app.tenant_id', ?, true)", String.class, societyId.toString());
        List<OutboxMessage> dueEvents = jdbcTemplate.query(
                """
                SELECT id, tenant_id, event_type, payload::text AS payload, created_at
                FROM outbox
                WHERE id = ? AND tenant_id = ? AND processed_at IS NULL AND next_attempt_at <= now()
                FOR UPDATE
                """,
                (resultSet, rowNumber) -> mapMessage(resultSet),
                eventId,
                societyId);
        if (dueEvents.isEmpty()) {
            return false;
        }

        OutboxMessage message = dueEvents.getFirst();
        List<OutboxEventHandler> handlers = handlersByEventType.getOrDefault(message.eventType(), List.of());
        if (handlers.isEmpty()) {
            throw new IllegalStateException("No handler registered for outbox event type " + message.eventType());
        }

        for (OutboxEventHandler handler : handlers) {
            int newReceipt = jdbcTemplate.update(
                    """
                    INSERT INTO outbox_handler_receipt (tenant_id, event_id, handler_key)
                    VALUES (?, ?, ?)
                    ON CONFLICT (tenant_id, event_id, handler_key) DO NOTHING
                    """,
                    societyId,
                    eventId,
                    handler.handlerKey());
            if (newReceipt == 1) {
                handler.handle(message);
            }
        }

        jdbcTemplate.update(
                "UPDATE outbox SET processed_at = now() WHERE id = ? AND tenant_id = ? AND processed_at IS NULL",
                eventId,
                societyId);
        return true;
    }

    private OutboxMessage mapMessage(ResultSet resultSet) throws SQLException {
        try {
            JsonNode payload = objectMapper.readTree(resultSet.getString("payload"));
            return new OutboxMessage(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("tenant_id", UUID.class),
                    resultSet.getString("event_type"),
                    payload,
                    resultSet.getTimestamp("created_at").toInstant());
        } catch (JacksonException exception) {
            throw new SQLException("Stored outbox payload is not valid JSON", exception);
        }
    }

    private Map<String, List<OutboxEventHandler>> indexHandlers(List<OutboxEventHandler> handlers) {
        Map<String, List<OutboxEventHandler>> indexed = new HashMap<>();
        for (OutboxEventHandler handler : handlers) {
            if (handler.eventType() == null || handler.eventType().isBlank()) {
                throw new IllegalStateException("Outbox handler eventType must not be blank");
            }
            if (handler.handlerKey() == null || handler.handlerKey().isBlank()) {
                throw new IllegalStateException("Outbox handler handlerKey must not be blank");
            }
            indexed.computeIfAbsent(handler.eventType(), ignored -> new java.util.ArrayList<>()).add(handler);
        }
        indexed.forEach((eventType, eventHandlers) -> {
            long distinctKeys = eventHandlers.stream().map(OutboxEventHandler::handlerKey).distinct().count();
            if (distinctKeys != eventHandlers.size()) {
                throw new IllegalStateException("Duplicate outbox handler key for event type " + eventType);
            }
        });
        indexed.replaceAll((eventType, eventHandlers) -> List.copyOf(eventHandlers));
        return Map.copyOf(indexed);
    }
}
