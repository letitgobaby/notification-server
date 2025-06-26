package notification.domain.vo;

import java.util.Objects;

import notification.definition.annotations.ValueObject;
import notification.definition.enums.NotificationType;

@ValueObject
public record NotificationChannelConfig(
        NotificationType notificationType,
        SenderInfo senderInfo) {

    public NotificationChannelConfig {
        Objects.requireNonNull(notificationType, "Notification type cannot be null in channel config");
        Objects.requireNonNull(senderInfo, "Sender info cannot be null in channel config");
    }
}