package notification.domain.notification.vo;

import java.util.Objects;
import java.util.UUID;

import notification.domain.common.annotations.ValueObject;

@ValueObject
public record NotificationId(UUID value) {

    public NotificationId {
        Objects.requireNonNull(value, "Notification Request ID cannot be null");
    }

    public static NotificationId generate() {
        return new NotificationId(UUID.randomUUID());
    }

    public static NotificationId fromString(String id) {
        return new NotificationId(UUID.fromString(id));
    }

}
