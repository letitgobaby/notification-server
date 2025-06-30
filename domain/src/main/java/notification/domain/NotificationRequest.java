package notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import notification.definition.annotations.AggregateRoot;
import notification.domain.enums.DeliveryStatus;
import notification.domain.enums.NotificationType;
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

    private DeliveryStatus status;
    private String failureReason; // 요청 처리 중 실패 사유
    private Instant processedAt; // 요청이 처리된 시각 (성공/실패 여부와 관계없이)
    private Instant createdAt;

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
            String memo, Instant scheduledAt, DeliveryStatus status, String failureReason, Instant processedAt,
            Instant createdAt) {
        this.requestId = requestId;
        this.requester = requester;
        this.recipients = recipients;
        this.notificationTypes = notificationTypes;
        this.senderInfos = senderInfos;
        this.content = content;
        this.template = template;
        this.memo = memo;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.failureReason = failureReason;
        this.processedAt = processedAt;
        this.createdAt = createdAt;
    }

}
