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
    @DisplayName("생성자는 모든 필드를 올바르게 설정해야 한다")
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
    @DisplayName("생성자는 필수 필드가 null일 때 MandatoryFieldException을 던져야 한다")
    public void constructor_shouldThrowMandatoryFieldException_whenNullFields() {
        assertThrows(MandatoryFieldException.class,
                () -> new RequestOutbox(null, aggregateId, payload, 0, now, OutboxStatus.PENDING, null, now));
        assertThrows(MandatoryFieldException.class,
                () -> new RequestOutbox(outboxId, null, payload, 0, now, OutboxStatus.PENDING, null, now));
        assertThrows(MandatoryFieldException.class,
                () -> new RequestOutbox(outboxId, aggregateId, null, 0, now, OutboxStatus.PENDING, null, now));
    }

    @Test
    @DisplayName("생성자는 재시도 횟수가 음수일 때 BusinessRuleViolationException을 던져야 한다")
    public void constructor_shouldThrowBusinessRuleViolationException_whenNegativeRetryAttempts() {
        assertThrows(BusinessRuleViolationException.class,
                () -> new RequestOutbox(outboxId, aggregateId, payload, -1, now, OutboxStatus.PENDING, null, now));
    }

    @Test
    @DisplayName("생성자는 nextRetryAt이 생성시간보다 60초 이상 이전일 때 BusinessRuleViolationException을 던져야 한다")
    public void constructor_shouldThrowBusinessRuleViolationException_whenNextRetryAtTooOld() {
        // nextRetryAt이 createdAt보다 60초 이상 이전이면 예외 발생
        Instant createdAt = now;
        Instant tooOldRetryAt = now.minusSeconds(61); // 60초보다 더 이전

        assertThrows(BusinessRuleViolationException.class,
                () -> new RequestOutbox(outboxId, aggregateId, payload, 0, tooOldRetryAt,
                        OutboxStatus.PENDING, null, createdAt));
    }

    @Test
    @DisplayName("생성자는 nextRetryAt이 60초 버퍼 이내일 때 예외를 던지지 않아야 한다")
    public void constructor_shouldNotThrow_whenNextRetryAtWithin60SecondBuffer() {
        // nextRetryAt이 createdAt보다 60초 이내 이전이면 허용
        Instant createdAt = now;
        Instant acceptableRetryAt = now.minusSeconds(59); // 60초 이내

        assertDoesNotThrow(() -> new RequestOutbox(outboxId, aggregateId, payload, 0, acceptableRetryAt,
                OutboxStatus.PENDING, null, createdAt));
    }

    @Test
    @DisplayName("create 메서드는 재시도 횟수가 0인 PENDING 상태의 Outbox를 반환해야 한다")
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
    @DisplayName("markAsFailed 메서드는 PENDING 상태에서 상태와 필드를 업데이트해야 한다")
    public void markAsFailed_shouldUpdateStatusAndFields_fromPending() {
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
    @DisplayName("markAsFailed 메서드는 IN_PROGRESS 상태에서 상태와 필드를 업데이트해야 한다")
    public void markAsFailed_shouldUpdateStatusAndFields_fromInProgress() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.IN_PROGRESS, now, now);

        Instant nextRetry = now.plusSeconds(60);
        outbox.markAsFailed(nextRetry);

        assertEquals(OutboxStatus.FAILED, outbox.getStatus());
        assertEquals(nextRetry, outbox.getNextRetryAt());
        assertEquals(1, outbox.getRetryAttempts());
        assertNotNull(outbox.getProcessedAt());
    }

    @Test
    @DisplayName("markAsFailed 메서드는 상태가 PENDING 또는 IN_PROGRESS가 아닐 때 예외를 던져야 한다")
    public void markAsFailed_shouldThrow_whenStatusNotPendingOrInProgress() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.FAILED, null, now);

        assertThrows(BusinessRuleViolationException.class, () -> outbox.markAsFailed(now.plusSeconds(20)));
    }

    @Test
    @DisplayName("markAsFailed 메서드는 nextRetryAt이 null이거나 과거일 때 예외를 던져야 한다")
    public void markAsFailed_shouldThrow_whenNextRetryAtNullOrPast() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.PENDING, null, now);

        assertThrows(BusinessRuleViolationException.class, () -> outbox.markAsFailed(null));
        assertThrows(BusinessRuleViolationException.class, () -> outbox.markAsFailed(now.minusSeconds(1)));
    }

    @Test
    @DisplayName("markAsInProgress 메서드는 상태를 IN_PROGRESS로 변경하고 처리시간을 설정해야 한다")
    public void markAsInProgress_shouldUpdateStatusAndProcessedAt() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.PENDING, null, now);

        outbox.markAsInProgress();

        assertEquals(OutboxStatus.IN_PROGRESS, outbox.getStatus());
        assertNotNull(outbox.getProcessedAt());
    }

    @Test
    @DisplayName("markAsInProgress 메서드는 상태가 PENDING이 아닐 때 예외를 던져야 한다")
    public void markAsInProgress_shouldThrow_whenStatusNotPending() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.FAILED, now, now);

        assertThrows(BusinessRuleViolationException.class, () -> outbox.markAsInProgress());
    }

    @Test
    @DisplayName("markAsSent 메서드는 PENDING 상태에서 SENT로 변경하고 처리시간을 설정해야 한다")
    public void markAsSent_shouldUpdateStatusAndProcessedAt_fromPending() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.PENDING, null, now);

        outbox.markAsSent();

        assertEquals(OutboxStatus.SENT, outbox.getStatus());
        assertNotNull(outbox.getProcessedAt());
    }

    @Test
    @DisplayName("markAsSent 메서드는 IN_PROGRESS 상태에서 SENT로 변경하고 처리시간을 설정해야 한다")
    public void markAsSent_shouldUpdateStatusAndProcessedAt_fromInProgress() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.IN_PROGRESS, now, now);

        outbox.markAsSent();

        assertEquals(OutboxStatus.SENT, outbox.getStatus());
        assertNotNull(outbox.getProcessedAt());
    }

    @Test
    @DisplayName("markAsSent 메서드는 상태가 PENDING 또는 IN_PROGRESS가 아닐 때 예외를 던져야 한다")
    public void markAsSent_shouldThrow_whenStatusNotPendingOrInProgress() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.FAILED, now, now);

        assertThrows(BusinessRuleViolationException.class, () -> outbox.markAsSent());
    }

    @Test
    @DisplayName("isMaxRetryAttemptsReached 메서드는 최대 재시도 횟수에 도달했을 때 true를 반환해야 한다")
    public void isMaxRetryAttemptsReached_shouldReturnTrueWhenReached() {
        RequestOutbox outbox = new RequestOutbox(
                outboxId, aggregateId, payload, 3, now.plusSeconds(10),
                OutboxStatus.FAILED, now, now);

        assertTrue(outbox.isMaxRetryAttemptsReached(3));
        assertTrue(outbox.isMaxRetryAttemptsReached(2));
        assertFalse(outbox.isMaxRetryAttemptsReached(4));
    }

    @Test
    @DisplayName("isMaxRetryAttemptsReached 메서드는 다양한 상태에서 동작해야 한다")
    public void isMaxRetryAttemptsReached_shouldWorkWithDifferentStatuses() {
        RequestOutbox pendingOutbox = new RequestOutbox(
                outboxId, aggregateId, payload, 2, now.plusSeconds(10),
                OutboxStatus.PENDING, null, now);
        RequestOutbox inProgressOutbox = new RequestOutbox(
                outboxId, aggregateId, payload, 3, now.plusSeconds(10),
                OutboxStatus.IN_PROGRESS, now, now);

        assertTrue(pendingOutbox.isMaxRetryAttemptsReached(2));
        assertFalse(pendingOutbox.isMaxRetryAttemptsReached(3));
        assertTrue(inProgressOutbox.isMaxRetryAttemptsReached(3));
        assertFalse(inProgressOutbox.isMaxRetryAttemptsReached(4));
    }

    @Test
    @DisplayName("isPending과 isFailed 메서드는 각 상태에 대해 올바르게 동작해야 한다")
    public void isPending_and_isFailed_shouldWork() {
        RequestOutbox pending = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.PENDING, null, now);
        RequestOutbox failed = new RequestOutbox(
                outboxId, aggregateId, payload, 1, now.plusSeconds(10),
                OutboxStatus.FAILED, now, now);
        RequestOutbox inProgress = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.IN_PROGRESS, now, now);
        RequestOutbox sent = new RequestOutbox(
                outboxId, aggregateId, payload, 0, now.plusSeconds(10),
                OutboxStatus.SENT, now, now);

        assertTrue(pending.isPending());
        assertFalse(pending.isFailed());
        assertFalse(failed.isPending());
        assertTrue(failed.isFailed());
        assertFalse(inProgress.isPending());
        assertFalse(inProgress.isFailed());
        assertFalse(sent.isPending());
        assertFalse(sent.isFailed());
    }
}