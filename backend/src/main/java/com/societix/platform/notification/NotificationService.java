package com.societix.platform.notification;

import com.societix.platform.outbox.OutboxEvent;
import com.societix.platform.outbox.OutboxWriter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final Set<String> channelKeys;

    public NotificationService(
            JdbcTemplate jdbcTemplate,
            OutboxWriter outboxWriter,
            JsonMapper jsonMapper,
            List<NotificationChannel> channels) {
        this.jdbcTemplate = jdbcTemplate;
        this.outboxWriter = outboxWriter;
        this.jsonMapper = jsonMapper;
        this.channelKeys = channels.stream().map(NotificationChannel::channelKey).collect(Collectors.toUnmodifiableSet());
    }

    /** Enqueues the request inside the caller's transaction, establishing RLS tenant context first. */
    @Transactional
    public UUID notifyResident(NotificationRequest request) {
        if (!channelKeys.contains(request.channelKey())) {
            throw new IllegalArgumentException("Unsupported notification channel");
        }
        String currentTenant = jdbcTemplate.queryForObject(
                "select coalesce(current_setting('app.tenant_id', true), '')", String.class);
        if (!currentTenant.isBlank()) {
            UUID boundTenant;
            try {
                boundTenant = UUID.fromString(currentTenant);
            } catch (IllegalArgumentException invalidTenant) {
                throw new IllegalStateException("Current transaction has an invalid tenant context");
            }
            if (!boundTenant.equals(request.societyId())) {
                throw new IllegalStateException("Notification society does not match the transaction tenant");
            }
        }
        jdbcTemplate.queryForObject(
                "select set_config('app.tenant_id', ?, true)", String.class, request.societyId().toString());
        return outboxWriter.append(new OutboxEvent(
                request.societyId(), EVENT_TYPE, request.toPayload(jsonMapper)));
    }
}
