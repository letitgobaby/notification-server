package notification.domain.vo;

import java.util.UUID;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.MandatoryFieldException;

@ValueObject
public record NotificationRequestId(String value) {

    public NotificationRequestId {
        if (value == null || value.isBlank()) {
            throw new MandatoryFieldException("Notification request ID cannot be null or blank");
        }
    }

    public static NotificationRequestId create() {
        return new NotificationRequestId(UUID.randomUUID().toString());
    }

    public static NotificationRequestId of(String value) {
        return new NotificationRequestId(value);
    }

}
