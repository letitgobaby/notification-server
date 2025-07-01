package notification.application.outbox;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import notification.definition.enums.OutboxStatus;
import notification.definition.exceptions.MandatoryFieldException;
import notification.definition.exceptions.PolicyViolationException;
import notification.definition.vo.JsonPayload;
import notification.definition.vo.outbox.MessageOutbox;
import notification.definition.vo.outbox.OutboxId;

class MessageOutboxTest {

    private OutboxId outboxId;
    private String aggregateId;
    private JsonPayload payload;
    private Instant now;

    @BeforeEach
    void setUp() {
        outboxId = OutboxId.generate();
        aggregateId = "req-123";
        payload = new JsonPayload("{\"foo\":\"bar\"}");
        now = Instant.now();
    }

    @Test
    void constructor_shouldCreateMessageOutbox_whenAllFieldsValid() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(60), OutboxStatus.PENDING, null, now);
        assertEquals(outboxId, msg.getOutboxId());
        assertEquals(aggregateId, msg.getAggregateId());
        assertEquals(payload, msg.getPayload());
        assertEquals(0, msg.getRetryAttempts());
        assertEquals(now.plusSeconds(60), msg.getNextRetryAt());
        assertEquals(OutboxStatus.PENDING, msg.getStatus());
        assertNull(msg.getProcessedAt());
        assertEquals(now, msg.getCreatedAt());
    }

    @Test
    void constructor_shouldThrowMandatoryFieldNullException_whenAnyMandatoryFieldIsNull() {

        assertThrows(MandatoryFieldException.class, () -> {
            new MessageOutbox(null, aggregateId, payload,
                    0, now, OutboxStatus.PENDING, null, now);
        });

        assertThrows(MandatoryFieldException.class, () -> {
            new MessageOutbox(outboxId, null, payload,
                    0, now, OutboxStatus.PENDING, null, now);
        });

        assertThrows(MandatoryFieldException.class, () -> {
            new MessageOutbox(outboxId, aggregateId, null,
                    0, now, OutboxStatus.PENDING, null, now);
        });
    }

    @Test
    void constructor_shouldThrowFieldValidationException_whenNextRetryAtBeforeCreatedAt() {
        Instant createdAt = now;
        Instant nextRetryAt = now.minusSeconds(10);

        assertThrows(PolicyViolationException.class, () -> {
            new MessageOutbox(outboxId, aggregateId, payload,
                    0, nextRetryAt, OutboxStatus.PENDING, null, createdAt);
        });
    }

    @Test
    void constructor_shouldThrowFieldValidationException_whenRetryAttemptsNegative() {
        assertThrows(PolicyViolationException.class, () -> {
            new MessageOutbox(outboxId, aggregateId, payload,
                    -1, now, OutboxStatus.PENDING, null, now);
        });
    }

    @Test
    void create_shouldReturnPendingMessageOutbox() {
        MessageOutbox msg = MessageOutbox.create(aggregateId, payload, now.plusSeconds(100));
        assertEquals(OutboxStatus.PENDING, msg.getStatus());
        assertEquals(0, msg.getRetryAttempts());
        assertTrue(msg.isPending());
        assertFalse(msg.isFailed());
    }

    @Test
    void markAsFailed_shouldUpdateStatusAndFields_whenPendingAndValidNextRetryAt() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.PENDING, null, now);
        Instant retryAt = Instant.now().plusSeconds(60);
        msg.markAsFailed(retryAt);

        assertEquals(OutboxStatus.FAILED, msg.getStatus());
        assertEquals(retryAt, msg.getNextRetryAt());
        assertEquals(1, msg.getRetryAttempts());
        assertNotNull(msg.getProcessedAt());
        assertTrue(msg.isFailed());
        assertFalse(msg.isPending());
    }

    @Test
    void markAsFailed_shouldThrow_whenStatusIsNotPending() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.FAILED, now, now);
        assertThrows(PolicyViolationException.class, () -> msg.markAsFailed(Instant.now().plusSeconds(10)));
    }

    @Test
    void markAsFailed_shouldThrow_whenNextRetryAtIsNullOrPast() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.PENDING, null, now);
        assertThrows(PolicyViolationException.class, () -> msg.markAsFailed(null));
        assertThrows(PolicyViolationException.class, () -> msg.markAsFailed(Instant.now().minusSeconds(10)));
    }

    @Test
    void isMaxRetryAttemptsReached_shouldReturnTrue_whenRetryAttemptsGreaterThanOrEqualToMax() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                3, now.plusSeconds(10), OutboxStatus.FAILED, now, now);
        assertTrue(msg.isMaxRetryAttemptsReached(3));
        assertTrue(msg.isMaxRetryAttemptsReached(2));
        assertFalse(msg.isMaxRetryAttemptsReached(4));
    }

    @Test
    void isMaxRetryAttemptsReached_shouldThrow_whenStatusIsNotFailed() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                1, now.plusSeconds(10), OutboxStatus.PENDING, null, now);
        assertThrows(PolicyViolationException.class, () -> msg.isMaxRetryAttemptsReached(3));
    }

    @Test
    void isPending_and_isFailed_shouldReturnCorrectStatus() {
        MessageOutbox pendingMsg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.PENDING, null, now);
        MessageOutbox failedMsg = new MessageOutbox(outboxId, aggregateId, payload,
                1, now.plusSeconds(10), OutboxStatus.FAILED, now, now);
        assertTrue(pendingMsg.isPending());
        assertFalse(pendingMsg.isFailed());
        assertFalse(failedMsg.isPending());
        assertTrue(failedMsg.isFailed());
    }
}