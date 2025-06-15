package notification.domain.notification;

import java.time.Instant;
import java.util.Objects;

import lombok.Getter;
import notification.domain.common.annotations.AggregateRoot;
import notification.domain.common.exceptions.DomainFieldNullException;
import notification.domain.common.exceptions.DomainValidationException;
import notification.domain.notification.enums.AudienceType;
import notification.domain.notification.enums.NotificationType;
import notification.domain.notification.enums.Priority;
import notification.domain.notification.enums.RequesterType;
import notification.domain.notification.vo.EmailContent;
import notification.domain.notification.vo.EmailRecipient;
import notification.domain.notification.vo.NotificationContent;
import notification.domain.notification.vo.NotificationId;
import notification.domain.notification.vo.PushContent;
import notification.domain.notification.vo.PushRecipient;
import notification.domain.notification.vo.Recipient;
import notification.domain.notification.vo.RequesterId;
import notification.domain.notification.vo.SmsContent;
import notification.domain.notification.vo.SmsRecipient;

@AggregateRoot
@Getter
public class NotificationRequest {

    private final NotificationId notificationId;

    private final NotificationType type; // 알림 유형 (예: SMS, PUSH, EMAIL, CHAT 등)
    private final RequesterType requesterType; // 요청자 유형 (예: USER, SYSTEM, ADMIN 등)
    private final RequesterId requesterId; // 요청자 ID (예: 사용자 ID, 시스템 ID 등)

    private final AudienceType audienceType; // 대상 유형 (예: INDIVIDUAL, GROUP, BROADCAST 등)
    private final NotificationContent content; // 템플릿 ID, 템플릿 변수, 이미지 URL, 딥링크 등
    private final Recipient recipient; // 수신자 식별 정보 (예: userIds, chatRoomId, emailAddress, phoneNumber)

    private Instant scheduledAt;
    private final Priority priority;
    private final Instant requestedAt;

    public NotificationRequest(NotificationId notificationId, RequesterId requesterId,
            RequesterType requesterType, NotificationType type, AudienceType audienceType, Recipient recipient,
            NotificationContent content, Instant scheduledAt, Priority priority, Instant requestedAt) {

        try {
            this.notificationId = Objects.requireNonNull(notificationId, "Notification ID cannot be null");
            this.requesterId = Objects.requireNonNull(requesterId, "Requester ID cannot be null");
            this.requesterType = Objects.requireNonNull(requesterType, "Requester type cannot be null");
            this.type = Objects.requireNonNull(type, "Notification type cannot be null");
            this.audienceType = Objects.requireNonNull(audienceType, "Audience type cannot be null");
            this.recipient = Objects.requireNonNull(recipient, "Recipient info cannot be null");
            this.content = Objects.requireNonNull(content, "Payload cannot be null");
            this.scheduledAt = scheduledAt; // 예약 발송 시간은 null일 수 있음
            this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
            this.requestedAt = Objects.requireNonNull(requestedAt, "Requested time cannot be null");
        } catch (NullPointerException e) {
            throw new DomainFieldNullException(e.getMessage(), e);
        }

        // 비즈니스 규칙 검증
        validateBusinessRules();
    }

    private void validateBusinessRules() {
        if (scheduledAt != null && scheduledAt.isBefore(Instant.now().minusSeconds(1))) {
            throw new DomainValidationException("Scheduled time cannot be in the past");
        }

        if (!isContentAndRecipientTypeConsistent(this.type, this.content, this.recipient)) {
            throw new DomainValidationException(
                    "Notification type, content type, and recipient type are inconsistent.");
        }
    }

    private boolean isContentAndRecipientTypeConsistent(
            NotificationType type, NotificationContent content, Recipient recipient) {
        return switch (type) {
            case SMS -> content instanceof SmsContent && recipient instanceof SmsRecipient;
            case PUSH -> content instanceof PushContent && recipient instanceof PushRecipient;
            case EMAIL -> content instanceof EmailContent && recipient instanceof EmailRecipient;
            default -> false;
        };
    }

}
