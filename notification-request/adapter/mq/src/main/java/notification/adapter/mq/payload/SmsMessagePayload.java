package notification.adapter.mq.payload;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class SmsMessagePayload implements NotificationMessagePayload {
    private String messageId;
    private String requestId;
    private Instant createdAt;

    private String senderPhone;
    private String recipientPhone;
    private String messageText;

    @Override
    public String getNotificationType() {
        return "SMS";
    }

}
