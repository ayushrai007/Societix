package com.societix.platform.notification;

import java.util.Objects;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** A request from a module to notify one resident. Message content is kept out of logs. */
public record NotificationRequest(UUID societyId, UUID residentId, String title, String body) {

    public NotificationRequest {
        Objects.requireNonNull(societyId, "societyId must not be null");
        Objects.requireNonNull(residentId, "residentId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(body, "body must not be null");
        if (title.isBlank() || body.isBlank()) {
            throw new IllegalArgumentException("title and body must not be blank");
        }
    }

    JsonNode toPayload(tools.jackson.databind.json.JsonMapper mapper) {
        return mapper.createObjectNode()
                .put("residentId", residentId.toString())
                .put("title", title)
                .put("body", body);
    }
}
