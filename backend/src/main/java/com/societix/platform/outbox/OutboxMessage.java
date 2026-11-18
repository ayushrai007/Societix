package com.societix.platform.outbox;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Persisted event data delivered to registered platform and domain handlers. */
public record OutboxMessage(UUID id, UUID societyId, String eventType, JsonNode payload, Instant createdAt) {}
