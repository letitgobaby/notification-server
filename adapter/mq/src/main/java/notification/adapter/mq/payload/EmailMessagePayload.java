package notification.adapter.mq.payload;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class EmailMessagePayload implements NotificationMessagePayload {
    private String messageId;
    private String requestId;
    private Instant createdAt;

    private String subject;
    private String body;
    private String senderName;
    private String senderEmail;
    private String recipientEmail;

    @Override
    public String getNotificationType() {
        return "EMAIL";
    }

}
