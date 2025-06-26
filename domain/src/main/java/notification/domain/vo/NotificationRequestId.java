package notification.domain.vo;

import java.util.UUID;

import notification.definition.annotations.ValueObject;

@ValueObject
public record NotificationRequestId(String value) {

    public NotificationRequestId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NotificationRequestId cannot be null or blank");
        }
    }

    public static NotificationRequestId generate() {
        return new NotificationRequestId(UUID.randomUUID().toString());
    }

    public static NotificationRequestId of(String value) {
        return new NotificationRequestId(value);
    }

}
