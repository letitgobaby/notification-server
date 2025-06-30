package notification.domain.vo;

import notification.definition.annotations.ValueObject;

@ValueObject
public record UserId(String value) {
    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
    }

    public static UserId of(String value) {
        return new UserId(value);
    }

}
