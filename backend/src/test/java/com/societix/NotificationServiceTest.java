package com.societix;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.societix.platform.notification.NotificationChannel;
import com.societix.platform.notification.NotificationRequest;
import com.societix.platform.notification.NotificationService;
import com.societix.platform.outbox.OutboxWriter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

class NotificationServiceTest {

    @Test
    void unsupportedChannelIsRejectedBeforeWritingToTheOutbox() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        OutboxWriter outboxWriter = mock(OutboxWriter.class);
        NotificationChannel inApp = mock(NotificationChannel.class);
        when(inApp.channelKey()).thenReturn(NotificationChannel.IN_APP);
        NotificationService service = new NotificationService(
                jdbcTemplate, outboxWriter, JsonMapper.builder().build(), List.of(inApp));

        assertThatThrownBy(() -> service.notifyResident(new NotificationRequest(
                        UUID.randomUUID(), UUID.randomUUID(), "sms", "Notice", "Service update")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported notification channel");
        verifyNoInteractions(jdbcTemplate, outboxWriter);
    }
}
