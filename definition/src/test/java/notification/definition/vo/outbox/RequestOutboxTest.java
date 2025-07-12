package notification.definition.vo.outbox;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import notification.definition.enums.OutboxStatus;
import notification.definition.exceptions.BusinessRuleViolationException;
import notification.definition.exceptions.MandatoryFieldException;
import notification.definition.vo.JsonPayload;

public class RequestOutboxTest {

    private OutboxId outboxId;
    private String aggregateId;
    private JsonPayload payload;
    private Instant now;

    @BeforeEach
    public void setUp() {
        outboxId = OutboxId.generate();
        aggregateId = "agg-123";
        payload = new JsonPayload("{\"foo\":\"bar\"}");
        now = Instant.now();
    }

    @Test
    public void constructor_shouldSetFieldsCorrectly() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(60),
                OutboxStatus.PENDING, null, now);

        assertEquals(outboxId, outbox.getOutboxId());
        assertEquals(aggregateId, outbox.getAggregateId());
        assertEquals(payload, outbox.getPayload());
        assertEquals(0, outbox.getRetryAttempts());
        assertEquals(now.plusSeconds(60), outbox.getNextRetryAt());
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());
        assertNull(outbox.getProcessedAt());
        assertEquals(now, outbox.getCreatedAt());
    }

    @Test
    public void constructor_shouldThrowMandatoryFieldException_whenNullFields() {
        assertThrows(MandatoryFieldException.class,
                () -> new RequestOutbox(null, aggregateId, payload, 0, now, OutboxStatus.PENDING, null, now));
        assertThrows(MandatoryFieldException.class,
                () -> new RequestOutbox(outboxId, null, payload, 0, now, OutboxStatus.PENDING, null, now));
        assertThrows(MandatoryFieldException.class,
                () -> new RequestOutbox(outboxId, aggregateId, null, 0, now, OutboxStatus.PENDING, null, now));
    }

    @Test
    public void constructor_shouldThrowBusinessRuleViolationException_whenNegativeRetryAttempts() {
        assertThrows(BusinessRuleViolationException.class,
                () -> new RequestOutbox(outboxId, aggregateId, payload, -1, now, OutboxStatus.PENDING, null, now));
    }

    @Test
    public void constructor_shouldThrowBusinessRuleViolationException_whenNextRetryAtTooOld() {
        Instant old = Instant.now().minusSeconds(20);
        assertThrows(BusinessRuleViolationException.class,
                () -> new RequestOutbox(outboxId, aggregateId, payload, 0, old, OutboxStatus.PENDING, null, now));
    }

    @Test
    public void create_shouldReturnPendingOutboxWithZeroRetries() {
        Instant retryAt = now.plusSeconds(30);
        RequestOutbox outbox = RequestOutbox.create(aggregateId, payload, retryAt);

        assertEquals(aggregateId, outbox.getAggregateId());
        assertEquals(payload, outbox.getPayload());
        assertEquals(0, outbox.getRetryAttempts());
        assertEquals(retryAt, outbox.getNextRetryAt());
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());
        assertNull(outbox.getProcessedAt());
    }

    @Test
    public void markAsFailed_shouldUpdateStatusAndFields() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.PENDING, null, now);

        Instant nextRetry = now.plusSeconds(60);
        outbox.markAsFailed(nextRetry);

        assertEquals(OutboxStatus.FAILED, outbox.getStatus());
        assertEquals(nextRetry, outbox.getNextRetryAt());
        assertEquals(1, outbox.getRetryAttempts());
        assertNotNull(outbox.getProcessedAt());
    }

    @Test
    public void markAsFailed_shouldThrow_whenStatusNotPending() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.FAILED, null, now);

        assertThrows(BusinessRuleViolationException.class, () -> outbox.markAsFailed(now.plusSeconds(20)));
    }

    @Test
    public void markAsFailed_shouldThrow_whenNextRetryAtNullOrPast() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.PENDING, null, now);

        assertThrows(BusinessRuleViolationException.class, () -> outbox.markAsFailed(null));
        assertThrows(BusinessRuleViolationException.class, () -> outbox.markAsFailed(now.minusSeconds(1)));
    }

    @Test
    public void isMaxRetryAttemptsReached_shouldReturnTrueWhenReached() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 3, now.plusSeconds(10),
                OutboxStatus.FAILED, now, now);

        assertTrue(outbox.isMaxRetryAttemptsReached(3));
        assertTrue(outbox.isMaxRetryAttemptsReached(2));
        assertFalse(outbox.isMaxRetryAttemptsReached(4));
    }

    @Test
    public void isMaxRetryAttemptsReached_shouldThrow_whenStatusNotFailed() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 1, now.plusSeconds(10),
                OutboxStatus.PENDING, null, now);

        assertThrows(BusinessRuleViolationException.class, () -> outbox.isMaxRetryAttemptsReached(2));
    }

    @Test
    public void isPending_and_isFailed_shouldWork() {
        RequestOutbox pending = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.PENDING, null, now);
        RequestOutbox failed = new RequestOutbox(
                outboxId, aggregateId, payload, 1, now.plusSeconds(10),
                OutboxStatus.FAILED, now, now);

        assertTrue(pending.isPending());
        assertFalse(pending.isFailed());
        assertFalse(failed.isPending());
        assertTrue(failed.isFailed());
    }
}