package notification.definition.vo.outbox;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import notification.definition.enums.OutboxStatus;
import notification.definition.exceptions.BusinessRuleViolationException;
import notification.definition.exceptions.MandatoryFieldException;
import notification.definition.vo.JsonPayload;

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
    @DisplayName("생성자는 모든 필드가 유효할 때 MessageOutbox를 생성해야 한다")
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
    @DisplayName("생성자는 필수 필드가 null일 때 MandatoryFieldException을 던져야 한다")
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
    @DisplayName("생성자는 nextRetryAt이 createdAt보다 60초 이상 이전일 때 BusinessRuleViolationException을 던져야 한다")
    void constructor_shouldThrowFieldValidationException_whenNextRetryAtBeforeCreatedAt() {
        Instant createdAt = now;
        Instant tooOldRetryAt = now.minusSeconds(61); // 60초보다 더 이전

        assertThrows(BusinessRuleViolationException.class, () -> {
            new MessageOutbox(outboxId, aggregateId, payload,
                    0, tooOldRetryAt, OutboxStatus.PENDING, null, createdAt);
        });
    }

    @Test
    @DisplayName("생성자는 nextRetryAt이 60초 버퍼 이내일 때 예외를 던지지 않아야 한다")
    void constructor_shouldNotThrow_whenNextRetryAtWithin60SecondBuffer() {
        Instant createdAt = now;
        Instant acceptableRetryAt = now.minusSeconds(59); // 60초 이내

        assertDoesNotThrow(() -> new MessageOutbox(outboxId, aggregateId, payload,
                0, acceptableRetryAt, OutboxStatus.PENDING, null, createdAt));
    }

    @Test
    @DisplayName("생성자는 재시도 횟수가 음수일 때 BusinessRuleViolationException을 던져야 한다")
    void constructor_shouldThrowFieldValidationException_whenRetryAttemptsNegative() {
        assertThrows(BusinessRuleViolationException.class, () -> {
            new MessageOutbox(outboxId, aggregateId, payload,
                    -1, now, OutboxStatus.PENDING, null, now);
        });
    }

    @Test
    @DisplayName("create 메서드는 PENDING 상태의 MessageOutbox를 반환해야 한다")
    void create_shouldReturnPendingMessageOutbox() {
        MessageOutbox msg = MessageOutbox.create(aggregateId, payload, now.plusSeconds(100));
        assertEquals(OutboxStatus.PENDING, msg.getStatus());
        assertEquals(0, msg.getRetryAttempts());
        assertTrue(msg.isPending());
        assertFalse(msg.isFailed());
    }

    @Test
    @DisplayName("markAsFailed 메서드는 PENDING 상태에서 유효한 nextRetryAt으로 상태와 필드를 업데이트해야 한다")
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
    @DisplayName("markAsFailed 메서드는 IN_PROGRESS 상태에서 유효한 nextRetryAt으로 상태와 필드를 업데이트해야 한다")
    void markAsFailed_shouldUpdateStatusAndFields_whenInProgressAndValidNextRetryAt() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.IN_PROGRESS, now, now);
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
    @DisplayName("markAsFailed 메서드는 상태가 PENDING 또는 IN_PROGRESS가 아닐 때 예외를 던져야 한다")
    void markAsFailed_shouldThrow_whenStatusIsNotPending() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.FAILED, now, now);
        assertThrows(BusinessRuleViolationException.class, () -> msg.markAsFailed(Instant.now().plusSeconds(10)));
    }

    @Test
    @DisplayName("markAsFailed 메서드는 nextRetryAt이 null이거나 과거일 때 예외를 던져야 한다")
    void markAsFailed_shouldThrow_whenNextRetryAtIsNullOrPast() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.PENDING, null, now);
        assertThrows(BusinessRuleViolationException.class, () -> msg.markAsFailed(null));
        assertThrows(BusinessRuleViolationException.class, () -> msg.markAsFailed(Instant.now().minusSeconds(10)));
    }

    @Test
    @DisplayName("markAsInProgress 메서드는 상태를 IN_PROGRESS로 변경하고 처리시간을 설정해야 한다")
    void markAsInProgress_shouldUpdateStatusAndProcessedAt() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.PENDING, null, now);

        msg.markAsInProgress();

        assertEquals(OutboxStatus.IN_PROGRESS, msg.getStatus());
        assertNotNull(msg.getProcessedAt());
    }

    @Test
    @DisplayName("markAsInProgress 메서드는 상태가 PENDING이 아닐 때 예외를 던져야 한다")
    void markAsInProgress_shouldThrow_whenStatusNotPending() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.FAILED, now, now);

        assertThrows(BusinessRuleViolationException.class, () -> msg.markAsInProgress());
    }

    @Test
    @DisplayName("markAsSent 메서드는 PENDING 상태에서 SENT로 변경하고 처리시간을 설정해야 한다")
    void markAsSent_shouldUpdateStatusAndProcessedAt_fromPending() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.PENDING, null, now);

        msg.markAsSent();

        assertEquals(OutboxStatus.SENT, msg.getStatus());
        assertNotNull(msg.getProcessedAt());
    }

    @Test
    @DisplayName("markAsSent 메서드는 IN_PROGRESS 상태에서 SENT로 변경하고 처리시간을 설정해야 한다")
    void markAsSent_shouldUpdateStatusAndProcessedAt_fromInProgress() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.IN_PROGRESS, now, now);

        msg.markAsSent();

        assertEquals(OutboxStatus.SENT, msg.getStatus());
        assertNotNull(msg.getProcessedAt());
    }

    @Test
    @DisplayName("markAsSent 메서드는 상태가 PENDING 또는 IN_PROGRESS가 아닐 때 예외를 던져야 한다")
    void markAsSent_shouldThrow_whenStatusNotPendingOrInProgress() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.FAILED, now, now);

        assertThrows(BusinessRuleViolationException.class, () -> msg.markAsSent());
    }

    @Test
    @DisplayName("isMaxRetryAttemptsReached 메서드는 재시도 횟수가 최대값보다 크거나 같을 때 true를 반환해야 한다")
    void isMaxRetryAttemptsReached_shouldReturnTrue_whenRetryAttemptsGreaterThanOrEqualToMax() {
        MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
                3, now.plusSeconds(10), OutboxStatus.FAILED, now, now);
        assertTrue(msg.isMaxRetryAttemptsReached(3));
        assertTrue(msg.isMaxRetryAttemptsReached(2));
        assertFalse(msg.isMaxRetryAttemptsReached(4));
    }

    // @Test
    // void isMaxRetryAttemptsReached_shouldThrow_whenStatusIsNotFailed() {
    // MessageOutbox msg = new MessageOutbox(outboxId, aggregateId, payload,
    // 1, now.plusSeconds(10), OutboxStatus.PENDING, null, now);
    // assertThrows(BusinessRuleViolationException.class, () -> {
    // msg.isMaxRetryAttemptsReached(3);
    // });
    // }

    @Test
    @DisplayName("isMaxRetryAttemptsReached 메서드는 다양한 상태에서 동작해야 한다")
    void isMaxRetryAttemptsReached_shouldWorkWithDifferentStatuses() {
        MessageOutbox pendingMsg = new MessageOutbox(outboxId, aggregateId, payload,
                2, now.plusSeconds(10), OutboxStatus.PENDING, null, now);
        MessageOutbox inProgressMsg = new MessageOutbox(outboxId, aggregateId, payload,
                3, now.plusSeconds(10), OutboxStatus.IN_PROGRESS, now, now);

        assertTrue(pendingMsg.isMaxRetryAttemptsReached(2));
        assertFalse(pendingMsg.isMaxRetryAttemptsReached(3));
        assertTrue(inProgressMsg.isMaxRetryAttemptsReached(3));
        assertFalse(inProgressMsg.isMaxRetryAttemptsReached(4));
    }

    @Test
    @DisplayName("isPending과 isFailed 메서드는 올바른 상태를 반환해야 한다")
    void isPending_and_isFailed_shouldReturnCorrectStatus() {
        MessageOutbox pendingMsg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.PENDING, null, now);
        MessageOutbox failedMsg = new MessageOutbox(outboxId, aggregateId, payload,
                1, now.plusSeconds(10), OutboxStatus.FAILED, now, now);
        MessageOutbox inProgressMsg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.IN_PROGRESS, now, now);
        MessageOutbox sentMsg = new MessageOutbox(outboxId, aggregateId, payload,
                0, now.plusSeconds(10), OutboxStatus.SENT, now, now);

        assertTrue(pendingMsg.isPending());
        assertFalse(pendingMsg.isFailed());
        assertFalse(failedMsg.isPending());
        assertTrue(failedMsg.isFailed());
        assertFalse(inProgressMsg.isPending());
        assertFalse(inProgressMsg.isFailed());
        assertFalse(sentMsg.isPending());
        assertFalse(sentMsg.isFailed());
    }
}