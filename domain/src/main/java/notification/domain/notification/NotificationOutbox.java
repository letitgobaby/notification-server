package notification.domain.notification;

import java.time.Instant;
import java.util.Objects;

import lombok.Getter;
import notification.domain.common.annotations.AggregateRoot;
import notification.domain.common.exceptions.DomainFieldNullException;
import notification.domain.common.exceptions.DomainValidationException;
import notification.domain.common.vo.JsonPayload;
import notification.domain.notification.enums.OutboxStatus;
import notification.domain.notification.exceptions.InvalidOutboxStatusException;
import notification.domain.notification.vo.NotificationId;

@AggregateRoot
@Getter
public class NotificationOutbox {

    /**
     * NotificationOutbox는 애그리거트의 상태 변경 이벤트를 메시지 큐(MQ)로 전송하기 위한
     * 아웃박스 패턴을 구현한 클래스입니다.
     * 
     * 이 알림 Front 프로젝트에서는 aggregateType이 "NotificationRequest"로 고정되어 있으며,
     * messageType은 "RequestedEvent"로 고정되어 있습니다.
     * 
     * MQ의 토픽을 구분하기 위해 aggregateType과 messageType을 사용합니다.
     * ex) "NotificationRequest:RequestedEvent"
     */
    private final String aggregateType; // 어떤 애그리거트 타입과 관련된 메시지인지 (예: "NotificationRequest")
    private final String messageType; // 발행될 이벤트/메시지의 타입 (예: "RequestedEvent")

    /*   */
    private final Long outboxId;
    private final NotificationId notificationId; // 관련 애그리거트의 ID (NotificationRequest의 ID)
    private final JsonPayload payload; // MQ로 전송될 메시지 내용 (JSON 형태로 저장)
    private OutboxStatus status; // 메시지 처리 상태 (PENDING, SENT, FAILED)
    private int retryAttempts; // 재시도 횟수
    private Instant nextRetryAt; // 다음 재시도 예정 시각
    private String errorMessage; // 실패 시 에러 메시지 (선택적)
    private Instant createdAt; // 아웃박스에 메시지가 저장된 시각

    public NotificationOutbox(Long outboxId, String aggregateType, NotificationId notificationId,
            String messageType, JsonPayload payload, OutboxStatus status, int retryAttempts,
            Instant nextRetryAt, Instant createdAt) {

        try {
            this.aggregateType = "NotificationRequest"; // 애그리거트 타입은 NotificationRequest로 고정
            this.messageType = "RequestedEvent"; // 발행될 메시지 타입은 NotificationRequestedEvent로 고정

            if (outboxId != null && outboxId <= 0) {
                throw new DomainValidationException("Outbox ID must be positive if present.");
            }
            this.outboxId = outboxId; // DB에서 자동 생성되므로 null일 수 있음
            this.notificationId = Objects.requireNonNull(notificationId, "Notification ID cannot be null.");
            this.payload = Objects.requireNonNull(payload, "Payload cannot be null.");
            this.status = Objects.requireNonNull(status, "Status cannot be null.");
            this.nextRetryAt = Objects.requireNonNull(nextRetryAt, "Next retry time cannot be null.");
            this.retryAttempts = retryAttempts < 0 ? 0 : retryAttempts; // 재시도 횟수는 음수가 될 수 없음
            this.createdAt = createdAt;
            this.errorMessage = null; // 초기에는 에러 메시지가 없음
        } catch (NullPointerException e) {
            throw new DomainFieldNullException(e.getMessage(), e);
        }
    }

    /**
     * 메시지가 성공적으로 MQ로 전송되었을 때 호출되어 상태를 SENT로 변경합
     * PENDING 또는 FAILED 상태일 때만 SENT로 변경 가능
     * 
     * @throws InvalidOutboxStatusException 현재 상태가 PENDING 또는 FAILED가 아닌 경우
     */
    public void markAsSent() {
        if (this.status != OutboxStatus.PENDING && this.status != OutboxStatus.FAILED) {
            throw new InvalidOutboxStatusException(
                    "Cannot mark a message with status "
                            + this.status + " as FAILED. Only PENDING or FAILED messages can be marked as FAILED.");
        }
        this.status = OutboxStatus.SENT; // 상태를 SENT로 변경
    }

    /**
     * 
     * 메시지 전송에 실패했을 때 호출되어 상태를 FAILED로 변경하고 재시도 관련 정보를 업데이트
     * 
     * @param nextRetry 다음 재시도 시각 (null이 아니고 현재 시각 이후여야 함)
     * @throws DomainValidationException    nextRetry가 null이거나 현재 시각 이전인 경우
     * @throws InvalidOutboxStatusException 현재 상태가 PENDING 또는 FAILED가 아닌 경우
     */
    public void markAsFailed(Instant nextRetry) {
        if (this.status != OutboxStatus.PENDING && this.status != OutboxStatus.FAILED) {
            throw new InvalidOutboxStatusException(
                    "Cannot mark a message with status "
                            + this.status + " as FAILED. Only PENDING or FAILED messages can be marked as FAILED.");
        }

        if (nextRetry == null) {
            throw new DomainValidationException("Next retry time cannot be null when marking as FAILED.");
        }

        if (nextRetry.isBefore(Instant.now())) {
            throw new DomainValidationException("Next retry time must be in the future.");
        }

        this.status = OutboxStatus.FAILED;
        this.retryAttempts = this.retryAttempts + 1; // 재시도 횟수 증가
        this.nextRetryAt = nextRetry; // 다음 재시도 시각 설정
    }

    /**
     * 
     * 메시지가 영구적으로 실패하여 더 이상 재시도하지 않을 때 호출되어 상태를 DEAD로 변경
     * 
     * @param cause 실패 원인 예외 (null일 경우 기본 메시지 설정)
     */
    public void markAsDead(Exception cause) {
        this.status = OutboxStatus.DEAD;
        if (cause != null) {
            this.errorMessage = cause.getMessage(); // 실패 원인 메시지를 저장
        } else {
            this.errorMessage = "No error message provided"; // 예외가 없으면 기본 메시지 설정
        }
    }

    /**
     * 
     * 현재 상태가 PENDING인지 확인
     */
    public boolean isPending() {
        return this.status == OutboxStatus.PENDING;
    }

    /**
     * 
     * FAILED 상태이고, nextRetryAt 시각이 지났으면 재시도 가능
     */
    public boolean isReadyForRetry() {
        return this.status == OutboxStatus.FAILED && Instant.now().isAfter(this.nextRetryAt);
    }
}
