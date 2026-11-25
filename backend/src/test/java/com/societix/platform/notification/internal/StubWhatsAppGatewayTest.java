package com.societix.platform.notification.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StubWhatsAppGatewayTest {

    @Test
    void unconfiguredGatewayFailsDeliveryWithoutIncludingMessageContent() {
        StubWhatsAppGateway gateway = new StubWhatsAppGateway();

        assertThatThrownBy(() -> gateway.send(
                        UUID.randomUUID(), UUID.randomUUID(), "Private title", "Private body", UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("WhatsApp delivery is not configured");
    }
}
