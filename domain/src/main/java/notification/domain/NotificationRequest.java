package notification.domain;

import java.time.Instant;
import java.util.Objects;

import lombok.Getter;
import notification.definition.annotations.AggregateRoot;
import notification.definition.enums.RequestStatus;
import notification.definition.exceptions.MandatoryFieldException;
import notification.definition.exceptions.PolicyViolationException;
import notification.domain.vo.NotificationRequestDetails;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Requester;
import notification.domain.vo.TargetAudience;

@AggregateRoot
@Getter
public class NotificationRequest {
    private final NotificationRequestId requestId;
    private final Requester requester;
    private final TargetAudience targetAudience; // 대상 정보 추가
    private final NotificationRequestDetails requestDetails;
    private final Instant scheduledAt; // 알림 발송 예정 시각

    private RequestStatus status;
    private String failureReason; // 요청 처리 중 실패 사유
    private Instant processedAt; // 요청이 처리된 시각 (성공/실패 여부와 관계없이)

    private final Instant requestedAt; // 요청이 접수된 시각

    /**
     * 
     * NotificationRequest 생성자입니다.
     * 요청 ID, 요청자, 대상 정보, 요청 세부사항, 스케줄링 시각을 받아 새로운 요청을 생성합니다.
     * 
     * @param requestId      요청 ID
     * @param requester      요청자 정보
     * @param targetAudience 대상 정보
     * @param requestDetails 요청 세부사항
     * @param scheduledAt    알림 발송 예정 시각
     * @param status         요청 상태 (기본값: PENDING)
     * @param failureReason  실패 사유 (기본값: null)
     * @param processedAt    처리된 시각 (기본값: null)
     * @param requestedAt    요청이 접수된 시각 (기본값: 현재 시각)
     */
    public NotificationRequest(NotificationRequestId requestId, Requester requester,
            TargetAudience targetAudience, NotificationRequestDetails requestDetails, Instant scheduledAt,
            RequestStatus status, String failureReason, Instant processedAt, Instant requestedAt) {
        try {
            this.requestId = Objects.requireNonNull(requestId, "Request ID cannot be null");
            this.requester = Objects.requireNonNull(requester, "Requester cannot be null");
            this.targetAudience = Objects.requireNonNull(targetAudience, "Target audience cannot be null");
            this.requestDetails = Objects.requireNonNull(requestDetails, "Request details cannot be null");
            this.scheduledAt = scheduledAt; // 스케줄링 시각은 null일 수 있음
            this.status = Objects.requireNonNull(status, "Status cannot be null");
            this.failureReason = failureReason; // 실패 사유는 null일 수 있음
            this.processedAt = processedAt; // 처리된 시각은 null일 수 있음
            this.requestedAt = Objects.requireNonNull(requestedAt, "Requested time cannot be null");
        } catch (NullPointerException e) {
            throw new MandatoryFieldException(e.getMessage(), e);
        }

        if (scheduledAt != null && scheduledAt.isBefore(requestedAt)) {
            throw new PolicyViolationException("Scheduled time cannot be before requested time");
        }
    }

    /**
     * 
     * NotificationRequest를 생성하는 팩토리 메서드입니다.
     * 요청 ID, 요청자, 대상 정보, 요청 세부사항, 스케줄링 시각을 받아 새로운 요청을 생성합니다.
     * 
     * @param requestId      요청 ID
     * @param requester      요청자 정보
     * @param targetAudience 대상 정보
     * @param requestDetails 요청 세부사항
     * @param scheduledAt    알림 발송 예정 시각
     * @return 새로 생성된 NotificationRequest 인스턴스
     */
    public static NotificationRequest create(NotificationRequestId requestId, Requester requester,
            TargetAudience targetAudience, NotificationRequestDetails requestDetails, Instant scheduledAt) {
        return new NotificationRequest(requestId, requester, targetAudience, requestDetails, scheduledAt,
                RequestStatus.PENDING, null, null, Instant.now());
    }

    /**
     * 
     * 대량의 NotificationItem들을 생성하고 지연 메시지 큐에 스케줄링하는 중
     */
    public void markAsProcessing() {
        // 요청이 처리 중인 상태는 PENDING이어야 함
        if (status != RequestStatus.PENDING) {
            throw new PolicyViolationException("Cannot mark as processing unless status is PENDING");
        }
        this.status = RequestStatus.PROCESSING;
        this.processedAt = Instant.now(); // 처리된 시각 기록
    }

    /**
     * 
     * 요청에 포함된 모든 NotificationItem의 생성 및 스케줄링이 완료됨.
     * (여기서 '완료'는 모든 NotificationItem이 발송 시스템으로 '넘겨졌다'는 의미이지, 최종 수신자에게 '도달했다'는 의미는
     * 아닙니다.
     */
    public void markAsCompleted() {
        // 요청이 완료된 상태는 PROCESSING이어야 함
        if (status != RequestStatus.PROCESSING) {
            throw new PolicyViolationException("Cannot mark as completed unless status is PROCESSING");
        }
        this.status = RequestStatus.COMPLETED;
        this.processedAt = Instant.now(); // 처리된 시각 기록
    }

    /**
     * 요청을 실패로 표시합니다.
     * 이 메서드는 요청의 상태가 PROCESSING일 때만 호출할 수 있습니다.
     * 
     * @param reason 실패 사유
     */
    public void markAsFailed(String reason) {
        // 요청이 실패된 상태는 PROCESSING이어야 함
        if (status != RequestStatus.PROCESSING) {
            throw new PolicyViolationException("Cannot mark as failed unless status is PROCESSING");
        }
        this.status = RequestStatus.FAILED;
        this.failureReason = reason; // 실패 사유 기록
        this.processedAt = Instant.now(); // 처리된 시각 기록
    }

    /**
     * 요청을 취소로 표시합니다.
     * 이 메서드는 요청의 상태가 PENDING 또는 PROCESSING일 때만 호출할 수 있습니다.
     */
    public void markAsCanceled(String reason) {
        // 이미 완료된 알림은 취소할 수 없도록 방어 로직
        if (this.status == RequestStatus.COMPLETED || this.status == RequestStatus.FAILED) {
            throw new PolicyViolationException("Cannot cancel a notification request in status: " + this.status);
        }

        this.status = RequestStatus.CANCELED;
        this.failureReason = Objects.requireNonNull(reason, "Cancellation reason cannot be null"); // 사유 기록
        this.processedAt = Instant.now(); // 취소된 시각 기록
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    public boolean isProcessing() {
        return status == RequestStatus.PROCESSING;
    }

    public boolean isCompleted() {
        return status == RequestStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == RequestStatus.FAILED;
    }

    public boolean isCanceled() {
        return status == RequestStatus.CANCELED;
    }
}
