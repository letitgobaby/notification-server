package notification.application.idempotency;

import java.time.Instant;

import notification.definition.vo.JsonPayload;

public record Idempotency(
        String idempotencyKey,
        String operationType,
        JsonPayload data,
        Instant createdAt) {

}
