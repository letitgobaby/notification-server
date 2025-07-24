package notification.definition.vo.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import lombok.Getter;
import notification.definition.annotations.AggregateRoot;
import notification.definition.enums.OutboxStatus;
import notification.definition.exceptions.BusinessRuleViolationException;
import notification.definition.exceptions.MandatoryFieldException;
import notification.definition.vo.JsonPayload;

@Getter
@AggregateRoot
public class RequestOutbox {
    private final OutboxId outboxId;
    private final String aggregateId;
    private final JsonPayload payload;

    private OutboxStatus status; // 메시지 상태 (PENDING, FAILED)
    private Instant processedAt; // 처리된 시간 추가
    private int retryAttempts; // 재시도 횟수
    private Instant nextRetryAt; // 다음 재시도 예정 시각 == 알림 발송 시각
    private final Instant createdAt;

    /**
     * RequestOutbox 생성자입니다.
     *
     * @param outboxId      Outbox 메시지 ID
     * @param aggregateType 집계 타입 (예: NotificationRequest, NotificationMessage 등)
     * @param aggregateId   집계 ID (예: 요청 ID, 알림 항목 ID 등)
     * @param eventType     이벤트 타입 (예: NotificationRequestReceived 등)
     * @param payload       JSON 페이로드
     * @param retryAttempts 재시도 횟수
     * @param nextRetryAt   다음 재시도 예정 시각
     * @param status        메시지 상태 (기본값: PENDING)
     */
    public RequestOutbox(OutboxId outboxId, String aggregateId,
            JsonPayload payload, int retryAttempts, Instant nextRetryAt,
            OutboxStatus status, Instant processedAt, Instant createdAt) {
        try {
            this.outboxId = Objects.requireNonNull(outboxId, "Outbox ID cannot be null");
            this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate ID cannot be null");
            this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
            this.retryAttempts = retryAttempts; // 재시도 횟수는 음수일 수 없음
            this.nextRetryAt = nextRetryAt; // 다음 재시도 시각은 null일 수 있음
            this.status = status; // 기본값 PENDING
            this.processedAt = processedAt; // 처리된 시각은 null일 수 있음
            this.createdAt = createdAt; // 생성 시각은 현재 시각으로 초기화
        } catch (NullPointerException e) {
            throw new MandatoryFieldException(e.getMessage(), e);
        }

        /**
         * # 유효성 검사
         * nextRetryAt이 null이 아니고 createdAt이 null이 아닌 경우,
         * nextRetryAt이 createdAt 이전이면 예외 발생
         * 
         * createdAt은 DB 생성시점에 설정되므로, nextRetryAt이 먼저 생성됨
         * 스케줄링에 영향이 없도록 60초 버퍼를 둠
         */
        if (nextRetryAt != null && createdAt != null) {
            Duration gap = Duration.between(nextRetryAt, createdAt); // createdAt - nextRetryAt
            if (gap.compareTo(Duration.ofSeconds(60)) > 0) {
                throw new BusinessRuleViolationException(
                        "Next retry time is too early compared to created time (over 60 seconds difference)");
            }
        }

        if (retryAttempts < 0) {
            throw new BusinessRuleViolationException("Retry attempts cannot be negative");
        }
    }

    /**
     * MessageOutbox를 생성하는 정적 팩토리 메서드입니다.
     *
     * @param aggregateType 집계 타입 (예: NotificationRequest, NotificationMessage 등)
     * @param aggregateId   집계 ID (예: 요청 ID, 알림 항목 ID 등)
     * @param eventType     이벤트 타입 (예: NotificationRequestReceived 등)
     * @param payload       JSON 페이로드
     * @param nextRetryAt   다음 재시도 예정 시각
     * @return 새 RequestOutbox 인스턴스
     */
    public static RequestOutbox create(String aggregateId, JsonPayload payload, Instant nextRetryAt) {
        return new RequestOutbox(
                OutboxId.generate(),
                aggregateId, payload,
                0, nextRetryAt,
                OutboxStatus.PENDING, null, null);
    }

    /**
     * 메시지가 성공적으로 전송되었을 때 호출되어 상태를 SENT로 변경합니다.
     * PENDING 또는 IN_PROGRESS 상태에서만 호출 가능하며, 메시지를 삭제합니다.
     */
    public void markAsInProgress() {
        if (this.status != OutboxStatus.PENDING) {
            throw new BusinessRuleViolationException("Cannot mark as in progress when status is not PENDING");
        }

        this.status = OutboxStatus.IN_PROGRESS;
        this.processedAt = Instant.now(); // 처리된 시각은 현재 시각으로 설정
    }

    /**
     * 메세지 전송에 실패 했을때 호출되어 상태를 FAILED로 변경합니다.
     * PENDING 상태에서만 호출 가능하며, 다음 재시도 시각을 설정해야 합니다.
     * 
     * @param nextRetryAt
     */
    public void markAsFailed(Instant nextRetryAt) {
        if (this.status != OutboxStatus.PENDING && this.status != OutboxStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException("Cannot mark as failed when status is not PENDING");
        }

        if (nextRetryAt == null || nextRetryAt.isBefore(Instant.now())) {
            throw new BusinessRuleViolationException("Next retry time must be in the future");
        }

        this.status = OutboxStatus.FAILED;
        this.nextRetryAt = nextRetryAt; // 다음 재시도 시각 업데이트
        this.retryAttempts++; // 재시도 횟수 증가
        this.processedAt = Instant.now(); // 처리된 시각은 현재 시각으로 설정
    }

    /**
     * 메시지가 성공적으로 전송되었을 때 호출되어 상태를 SENT로 변경합니다.
     * IN_PROGRESS 상태에서만 호출 가능하며, 처리된 시각을 현재 시각으로 설정합니다.
     */
    public void markAsSent() {
        if (this.status != OutboxStatus.PENDING && this.status != OutboxStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException("Cannot mark as sent when status is not PENDING or IN_PROGRESS");
        }

        this.status = OutboxStatus.SENT;
        this.processedAt = Instant.now(); // 처리된 시각은 현재 시각으로 설정
    }

    /**
     * 메시지가 최대 재시도 횟수에 도달했는지 확인합니다.
     * 
     * @param maxRetries
     * @return
     */
    public boolean isMaxRetryAttemptsReached(int maxRetries) {
        return this.retryAttempts >= maxRetries;
    }

    public boolean isPending() {
        return this.status == OutboxStatus.PENDING;
    }

    public boolean isFailed() {
        return this.status == OutboxStatus.FAILED;
    }
}
