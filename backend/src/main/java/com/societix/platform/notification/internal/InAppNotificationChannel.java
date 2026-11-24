package com.societix.platform.notification.internal;

import com.societix.platform.notification.NotificationChannel;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Persists an in-app inbox item. The unique event key makes delivery safe on replay. */
@Component
public class InAppNotificationChannel implements NotificationChannel {

    private final JdbcTemplate jdbcTemplate;

    public InAppNotificationChannel(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String channelKey() {
        return "in-app";
    }

    @Override
    public void send(UUID societyId, UUID residentId, String title, String body, UUID eventId) {
        jdbcTemplate.update(
                """
                INSERT INTO resident_notification (tenant_id, event_id, resident_id, title, body)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, event_id) DO NOTHING
                """,
                societyId,
                eventId,
                residentId,
                title,
                body);
    }
}
