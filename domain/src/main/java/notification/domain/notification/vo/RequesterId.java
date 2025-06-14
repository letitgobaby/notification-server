package notification.domain.notification.vo;

import java.util.Objects;

import notification.domain.common.annotations.ValueObject;

@ValueObject
public record RequesterId(String id) {
    public RequesterId {
        Objects.requireNonNull(id, "Requester ID cannot be null");
    }

    public static RequesterId from(String id) {
        return new RequesterId(id);
    }

}
