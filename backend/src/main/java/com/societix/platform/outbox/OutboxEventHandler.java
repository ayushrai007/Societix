package com.societix.platform.outbox;

/**
 * Handles one event contract. Handler keys must remain stable across deployments.
 * Database effects must join the dispatch transaction; external effects must use the message ID
 * as their idempotency key because PostgreSQL cannot atomically commit a remote side effect.
 * Receipts deduplicate committed database effects only. If any handler for an event fails, the
 * transaction rolls back and every matching handler runs again on retry. Failed events are parked
 * after ten attempts, and later events may run while an earlier event is backing off.
 */
public interface OutboxEventHandler {

    String eventType();

    String handlerKey();

    void handle(OutboxMessage message);
}
