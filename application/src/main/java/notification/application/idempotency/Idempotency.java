package notification.application.idempotency;

import java.time.Instant;

import notification.definition.annotations.ValueObject;
import notification.definition.vo.JsonPayload;

@ValueObject
public record Idempotency(
        String idempotencyKey,
        String operationType,
        JsonPayload data,
        Instant createdAt) {

}
