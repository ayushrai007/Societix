package com.societix.platform.notification.internal;

import com.societix.platform.notification.NotificationChannel;
import com.societix.platform.notification.NotificationService;
import com.societix.platform.outbox.OutboxEventHandler;
import com.societix.platform.outbox.OutboxMessage;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Converts an outbox request into channel deliveries. */
@Component
public class NotificationRequestedHandler implements OutboxEventHandler {

    private static final String HANDLER_KEY = "platform-notification-requested-v1";
    private final Map<String, NotificationChannel> channelsByKey;

    public NotificationRequestedHandler(List<NotificationChannel> channels) {
        this.channelsByKey = channels.stream().collect(Collectors.toUnmodifiableMap(
                NotificationChannel::channelKey, Function.identity()));
    }

    @Override
    public String eventType() {
        return NotificationService.EVENT_TYPE;
    }

    @Override
    public String handlerKey() {
        return HANDLER_KEY;
    }

    @Override
    public void handle(OutboxMessage message) {
        UUID residentId = UUID.fromString(message.payload().required("residentId").stringValue());
        String channelKey = message.payload().required("channelKey").stringValue();
        String title = message.payload().required("title").stringValue();
        String body = message.payload().required("body").stringValue();
        NotificationChannel channel = channelsByKey.get(channelKey);
        if (channel == null) {
            throw new IllegalArgumentException("Unknown notification channel in outbox event");
        }
        channel.send(message.societyId(), residentId, title, body, message.id());
    }
}
