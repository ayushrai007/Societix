package com.societix.platform.outbox;

import java.util.Objects;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** A domain event ready to be recorded in the transactional outbox. */
public record OutboxEvent(UUID societyId, String eventType, JsonNode payload) {

    public OutboxEvent {
        Objects.requireNonNull(societyId, "societyId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
    }
}
