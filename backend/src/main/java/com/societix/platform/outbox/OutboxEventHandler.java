package com.societix.platform.outbox;

/**
 * Handles one event contract. Handler keys must remain stable across deployments.
 * Database effects must join the dispatch transaction; external effects must use the message ID
 * as their idempotency key because PostgreSQL cannot atomically commit a remote side effect.
 */
public interface OutboxEventHandler {

    String eventType();

    String handlerKey();

    void handle(OutboxMessage message);
}
