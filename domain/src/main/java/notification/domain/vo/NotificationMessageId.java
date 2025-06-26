package notification.domain.vo;

import java.util.UUID;

import notification.definition.annotations.ValueObject;

@ValueObject
public record NotificationMessageId(String value) {
    public NotificationMessageId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NotificationItemId cannot be null or blank");
        }
    }

    public static NotificationMessageId generate() {
        return new NotificationMessageId(UUID.randomUUID().toString());
    }

    public static NotificationMessageId of(String value) {
        return new NotificationMessageId(value);
    }

}
