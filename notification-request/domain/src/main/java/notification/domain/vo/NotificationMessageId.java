package notification.domain.vo;

import java.util.UUID;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.MandatoryFieldException;

@ValueObject
public record NotificationMessageId(String value) {
    public NotificationMessageId {
        if (value == null || value.isBlank()) {
            throw new MandatoryFieldException("Notification message ID cannot be null or blank");
        }
    }

    public static NotificationMessageId create() {
        return new NotificationMessageId(UUID.randomUUID().toString());
    }

    public static NotificationMessageId of(String value) {
        return new NotificationMessageId(value);
    }

}
