package notification.domain.event;

import java.time.Instant;

public record NotificationMessageEvent(
        String notificationMessageId,
        Instant scheduledAt) implements DomainEvent {

    @Override
    public String name() {
        return "NotificationMessage";
    }

}
