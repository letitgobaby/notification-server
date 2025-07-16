package notification.domain.vo;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.MandatoryFieldException;

@ValueObject
public record UserId(String value) {
    public UserId {
        if (value == null || value.isBlank()) {
            throw new MandatoryFieldException("User ID cannot be null or blank");
        }
    }

    public static UserId of(String value) {
        return new UserId(value);
    }

}
