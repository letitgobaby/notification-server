package notification.domain.event;

import java.time.Instant;

public record NotificationRequestEvent(
        String notificationRequestId,
        Instant scheduledAt) implements DomainEvent {

    @Override
    public String name() {
        return "NotificationRequest";
    }
}
