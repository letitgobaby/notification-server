package notification.domain;

import java.time.Instant;
import java.util.Objects;

import lombok.Getter;
import notification.definition.annotations.AggregateRoot;
import notification.definition.enums.DeliveryStatus;
import notification.definition.enums.NotificationType;
import notification.definition.exceptions.MandatoryFieldException;
import notification.definition.exceptions.PolicyViolationException;
import notification.definition.vo.UserId;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.UserNotificationHistoryId;

@AggregateRoot
@Getter
public class UserNotificationHistory {
    private final UserNotificationHistoryId userHistoryId; // 이 알림 내역의 고유 ID
    private final NotificationMessageId notificationMessageId; // 원본 NotificationMessage ID 참조
    private final NotificationRequestId notificationRequestId; // 원본 NotificationRequest ID 참조

    private final UserId userId; // 이 알림을 받은 사용자의 ID
    private final NotificationType notificationType; // 알림 채널 (SMS, EMAIL, PUSH)
    private final String title; // 알림 제목
    private final String content; // 알림 내용
    private String redirectUrl; // 알림 클릭 시 이동할 URL
    private String imageUrl; // 알림에 포함된 이미지 URL

    private final Instant sentAt; // 발송 시스템에 전달된 시간 (`NotificationItem.dispatchedAt`)
    private final boolean isRead; // 사용자가 읽었는지 여부
    private final DeliveryStatus deliveryStatus; // 실제 발송 상태 (PENDING, DISPATCHED, FAILED)
    private String deliveryFailureReason; // 발송 실패 사유

    /**
     * UserNotification 생성자입니다.
     *
     * @param userNotificationId    이 알림 내역의 고유 ID
     * @param userId                이 알림을 받은 사용자의 ID
     * @param NotificationMessageId 원본 NotificationMessage ID 참조
     * @param notificationRequestId 원본 NotificationRequest ID 참조
     * @param notificationType      알림 채널 (SMS, EMAIL, PUSH)
     * @param title                 알림 제목
     * @param content               알림 내용
     * @param sentAt                발송 시스템에 전달된 시간
     * @param isRead                사용자가 읽었는지 여부
     * @param redirectUrl           알림 클릭 시 이동할 URL
     * @param imageUrl              알림에 포함된 이미지 URL
     * @param deliveryStatus        실제 발송 상태 (PENDING, DISPATCHED, DELIVERED,
     *                              FAILED)
     */
    public UserNotificationHistory(UserNotificationHistoryId userHistoryId, UserId userId,
            NotificationMessageId notificationMessageId, NotificationRequestId notificationRequestId,
            NotificationType notificationType, String title, String content, Instant sentAt, boolean isRead,
            String redirectUrl, String imageUrl, DeliveryStatus deliveryStatus, String deliveryFailureReason) {
        try {
            this.userHistoryId = Objects.requireNonNull(userHistoryId, "UserNotification ID cannot be null");
            this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
            this.notificationMessageId = Objects.requireNonNull(notificationMessageId,
                    "NotificationItem ID cannot be null");
            this.notificationRequestId = Objects.requireNonNull(notificationRequestId,
                    "NotificationRequest ID cannot be null");
            this.notificationType = Objects.requireNonNull(notificationType, "Notification type cannot be null");
            this.title = Objects.requireNonNull(title, "Notification title cannot be null");
            this.content = Objects.requireNonNull(content, "Notification content cannot be null");
            this.sentAt = Objects.requireNonNull(sentAt, "Sent time cannot be null");
            this.isRead = Objects.requireNonNull(isRead, "Read status cannot be null");
            this.redirectUrl = redirectUrl;
            this.imageUrl = imageUrl;
            this.deliveryStatus = Objects.requireNonNull(deliveryStatus, "Delivery status cannot be null");
            this.deliveryFailureReason = deliveryFailureReason;
        } catch (NullPointerException e) {
            throw new MandatoryFieldException(e.getMessage(), e);
        }

        if (sentAt.isAfter(Instant.now())) {
            throw new PolicyViolationException("Sent time cannot be in the future");
        }

    }

    /**
     * UserNotification을 생성하는 팩토리 메서드입니다.
     *
     * @param userId                이 알림을 받은 사용자의 ID
     * @param notificationItemId    원본 NotificationItem ID 참조
     * @param notificationRequestId 원본 NotificationRequest ID 참조
     * @param notificationType      알림 채널 (SMS, EMAIL, PUSH)
     * @param title                 알림 제목
     * @param content               알림 내용
     * @param sentAt                발송 시스템에 전달된 시간
     * @param isRead                사용자가 읽었는지 여부
     * @param redirectUrl           알림 클릭 시 이동할 URL
     * @param imageUrl              알림에 포함된 이미지 URL
     * @param deliveryStatus        실제 발송 상태 (PENDING, DISPATCHED, DELIVERED,
     *                              FAILED)
     * @return 새로운 UserNotification 인스턴스
     */
    public static UserNotificationHistory create(UserId userId, NotificationMessageId notificationItemId,
            NotificationRequestId notificationRequestId, NotificationType notificationType, String title,
            String content, Instant sentAt, String redirectUrl, String imageUrl) {
        return new UserNotificationHistory(UserNotificationHistoryId.generate(), userId, notificationItemId,
                notificationRequestId, notificationType, title, content, sentAt, false, redirectUrl, imageUrl,
                DeliveryStatus.PENDING, null);
    }
}
