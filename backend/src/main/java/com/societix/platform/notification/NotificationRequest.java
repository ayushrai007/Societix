package com.societix.platform.notification;

import java.util.Objects;
import java.util.UUID;

/** A request from a module to notify one resident. Message content is kept out of logs. */
public record NotificationRequest(UUID societyId, UUID residentId, String channelKey, String title, String body) {

    public NotificationRequest {
        Objects.requireNonNull(societyId, "societyId must not be null");
        Objects.requireNonNull(residentId, "residentId must not be null");
        Objects.requireNonNull(channelKey, "channelKey must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(body, "body must not be null");
        if (channelKey.isBlank() || title.isBlank() || body.isBlank()) {
            throw new IllegalArgumentException("channelKey, title, and body must not be blank");
        }
    }

    tools.jackson.databind.JsonNode toPayload(tools.jackson.databind.json.JsonMapper mapper) {
        return mapper.createObjectNode()
                .put("residentId", residentId.toString())
                .put("channelKey", channelKey)
                .put("title", title)
                .put("body", body);
    }
}
