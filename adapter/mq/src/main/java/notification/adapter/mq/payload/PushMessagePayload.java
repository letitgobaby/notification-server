package notification.adapter.mq.payload;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class PushMessagePayload implements NotificationMessagePayload {
    private String messageId;
    private String requestId;
    private Instant createdAt;

    private String deviceToken;
    private String title;
    private String body;
    private String imageUrl;
    private String redirectUrl;

    private String senderName;

    @Override
    public String getNotificationType() {
        return "PUSH";
    }

}
