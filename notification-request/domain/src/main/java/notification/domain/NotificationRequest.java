package notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import notification.definition.annotations.AggregateRoot;
import notification.definition.exceptions.BusinessRuleViolationException;
import notification.definition.exceptions.MandatoryFieldException;
import notification.domain.enums.NotificationType;
import notification.domain.enums.RequestStatus;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Requester;
import notification.domain.vo.TemplateInfo;
import notification.domain.vo.recipient.RecipientReference;
import notification.domain.vo.sender.SenderInfo;

@Getter
@AggregateRoot
public class NotificationRequest {
    private final NotificationRequestId requestId;
    private final Requester requester;
    private final List<RecipientReference> recipients;
    private final List<NotificationType> notificationTypes;
    private final Map<NotificationType, SenderInfo> senderInfos;
    private final NotificationContent content;
    private final TemplateInfo template;
    private final Instant scheduledAt;
    private final String memo;

    private RequestStatus status;
    private String failureReason; // 요청 처리 중 실패 사유
    private Instant processedAt; // 요청이 처리된 시각 (성공/실패 여부와 관계없이)
    private Instant createdAt; // 영속화된 시각

    /**
     * NotificationRequest 생성자입니다.
     *
     * @param requestId         요청 ID
     * @param requester         요청자 정보
     * @param recipients        수신자 목록
     * @param notificationTypes 알림 채널 목록
     * @param senderInfos       발신자 정보 맵 (알림 채널별)
     * @param content           알림 내용
     * @param template          템플릿 정보
     * @param memo              메모
     * @param scheduledAt       알림 발송 예정 시각
     * @param status            요청 상태 (기본값: PENDING)
     * @param failureReason     실패 사유 (기본값: null)
     * @param processedAt       처리된 시각 (기본값: null)
     * @param createdAt         생성된 시각 (기본값: 현재 시각)
     */
    public NotificationRequest(NotificationRequestId requestId, Requester requester,
            List<RecipientReference> recipients, List<NotificationType> notificationTypes,
            Map<NotificationType, SenderInfo> senderInfos, NotificationContent content, TemplateInfo template,
            String memo, Instant scheduledAt, RequestStatus status, String failureReason, Instant processedAt,
            Instant createdAt) {
        try {
            this.requestId = Objects.requireNonNull(requestId, "Notification request ID cannot be null");
            this.requester = Objects.requireNonNull(requester, "Requester cannot be null");
            this.recipients = Objects.requireNonNull(recipients, "Recipients cannot be null");
            this.notificationTypes = Objects.requireNonNull(notificationTypes, "Notification types cannot be null");
            this.senderInfos = Objects.requireNonNull(senderInfos, "Sender infos cannot be null");
            this.content = content;
            this.template = template;
            this.status = Objects.requireNonNull(status, "Request status cannot be null");
            this.scheduledAt = scheduledAt;
            this.memo = memo;
            this.failureReason = failureReason;
            this.processedAt = processedAt;
            this.createdAt = createdAt;
        } catch (NullPointerException e) {
            throw new MandatoryFieldException("Mandatory fields cannot be null", e);
        }

        if (recipients.isEmpty()) {
            throw new BusinessRuleViolationException("Recipients cannot be empty");
        }

        if (notificationTypes.isEmpty()) {
            throw new BusinessRuleViolationException("Notification types cannot be empty");
        }

        if (senderInfos.isEmpty()) {
            throw new BusinessRuleViolationException("Sender infos cannot be empty");
        }

        if (content == null && template == null) {
            throw new BusinessRuleViolationException("At least one of content or template must be provided");
        }
    }

    /**
     * NotificationRequest 생성 팩토리 메서드입니다.
     *
     * @param requester         요청자 정보
     * @param recipients        수신자 목록
     * @param notificationTypes 알림 채널 목록
     * @param senderInfos       발신자 정보 맵 (알림 채널별)
     * @param content           알림 내용
     * @param template          템플릿 정보
     * @param memo              메모
     * @param scheduledAt       알림 발송 예정 시각
     */
    public static NotificationRequest create(Requester requester, List<RecipientReference> recipients,
            List<NotificationType> notificationTypes,
            Map<NotificationType, SenderInfo> senderInfos, NotificationContent content, TemplateInfo template,
            String memo, Instant scheduledAt) {
        return new NotificationRequest(NotificationRequestId.create(), requester,
                recipients, notificationTypes, senderInfos, content, template, memo, scheduledAt,
                RequestStatus.PENDING, null, null, null);
    }

    /**
     * 요청을 Processing 상태로 변경합니다.
     */
    public void markAsProcessing() {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("Cannot mark as processed when status is not PENDING");
        }

        this.status = RequestStatus.PROCESSING;
        this.processedAt = Instant.now();
    }

    /**
     * 요청을 성공적으로 처리한 후 DISPATCHED 상태로 변경합니다.
     * 요청이 PROCESSING 상태일 때만 DISPATCHED로 마크할 수 있습니다.
     */
    public void markAsDispatched() {
        if (this.status != RequestStatus.PROCESSING) {
            throw new IllegalStateException("Cannot mark as dispatched when status is not PROCESSING");
        }

        this.status = RequestStatus.DISPATCHED;
        this.processedAt = Instant.now();
    }

    /**
     * 요청을 실패 상태로 변경합니다.
     * 요청이 PROCESSING 상태일 때만 실패로 마크할 수 있습니다.
     *
     * @param reason 실패 사유
     */
    public void markAsFailed(String reason) {
        if (this.status != RequestStatus.PROCESSING) {
            throw new IllegalStateException("Cannot mark as failed when status is not PROCESSING");
        }

        this.status = RequestStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = Instant.now();
    }

    /**
     * 요청을 취소 상태로 변경합니다.
     * 요청이 PENDING 또는 PROCESSING 상태일 때만 취소할 수 있습니다.
     */
    public void markAsCanceled() {
        if (this.status != RequestStatus.PENDING && this.status != RequestStatus.PROCESSING) {
            throw new IllegalStateException("Cannot mark as cancelled when status is not PENDING or PROCESSING");
        }

        this.status = RequestStatus.CANCELED;
        this.processedAt = Instant.now();
    }

}
