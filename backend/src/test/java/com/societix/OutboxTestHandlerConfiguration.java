package com.societix;

import com.societix.platform.outbox.OutboxEventHandler;
import com.societix.platform.outbox.OutboxMessage;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
class OutboxTestHandlerConfiguration {

    @Bean
    RecordingOutboxEventHandler recordingOutboxEventHandler() {
        return new RecordingOutboxEventHandler();
    }

    static final class RecordingOutboxEventHandler implements OutboxEventHandler {

        private final ConcurrentHashMap<UUID, AtomicInteger> effectsByEvent = new ConcurrentHashMap<>();
        private final AtomicBoolean failNextDelivery = new AtomicBoolean();

        @Override
        public String eventType() {
            return "societix.test.idempotent";
        }

        @Override
        public String handlerKey() {
            return "test-recording-handler-v1";
        }

        @Override
        public void handle(OutboxMessage message) {
            if (failNextDelivery.compareAndSet(true, false)) {
                throw new IllegalStateException("intentional integration test failure");
            }
            effectsByEvent.computeIfAbsent(message.id(), ignored -> new AtomicInteger()).incrementAndGet();
        }

        void failNextDelivery() {
            failNextDelivery.set(true);
        }

        int effectCount(UUID eventId) {
            return effectsByEvent.getOrDefault(eventId, new AtomicInteger()).get();
        }
    }
}
