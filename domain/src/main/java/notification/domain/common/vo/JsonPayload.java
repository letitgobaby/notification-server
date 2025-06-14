package notification.domain.common.vo;

import notification.domain.common.annotations.ValueObject;
import notification.domain.common.exceptions.DomainValidationException;

@ValueObject
public record JsonPayload(String payload) {

    public JsonPayload {
        if (payload == null || payload.isBlank()) {
            throw new DomainValidationException("Payload cannot be null or empty");
        }
    }

    public static JsonPayload from(String payload) {
        return new JsonPayload(payload);
    }

}
