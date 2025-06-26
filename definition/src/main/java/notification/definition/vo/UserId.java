package notification.definition.vo;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.MandatoryFieldException;

@ValueObject
public record UserId(String value) {
    public UserId {
        if (value == null || value.isBlank()) {
            throw new MandatoryFieldException("UserId cannot be null or blank");
        }
    }

}
