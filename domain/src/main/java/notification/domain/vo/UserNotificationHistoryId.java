package notification.domain.vo;

import java.util.UUID;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.MandatoryFieldException;

@ValueObject
public record UserNotificationHistoryId(String value) {
    public UserNotificationHistoryId {
        if (value == null || value.isBlank()) {
            throw new MandatoryFieldException("UserNotificationId cannot be null or blank");
        }
    }

    public static UserNotificationHistoryId generate() {
        return new UserNotificationHistoryId(UUID.randomUUID().toString());
    }

}
