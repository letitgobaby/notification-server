package notification.domain.vo;

import notification.definition.annotations.ValueObject;

@ValueObject
public record OutboxId(String value) {

    public OutboxId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Outbox ID cannot be null or blank");
        }
    }

    public static OutboxId generate() {
        return new OutboxId(java.util.UUID.randomUUID().toString());
    }

}
