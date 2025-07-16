package notification.definition.vo;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.MandatoryFieldException;

@ValueObject
public record JsonPayload(String value) {
    public JsonPayload {
        if (value == null || value.isBlank()) {
            throw new MandatoryFieldException("JsonPayload cannot be null or empty");
        }
    }

    public static JsonPayload of(String value) {
        return new JsonPayload(value);
    }

}
