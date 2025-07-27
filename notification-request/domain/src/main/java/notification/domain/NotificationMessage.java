package notification.domain;

import java.time.Instant;
import java.util.Objects;

import lombok.Getter;
import notification.definition.annotations.AggregateRoot;
import notification.definition.exceptions.MandatoryFieldException;
import notification.domain.enums.DeliveryStatus;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.sender.SenderInfo;

@Getter
@AggregateRoot
public class NotificationMessage {
    private final NotificationMessageId messageId;
    private final NotificationRequestId requestId;
    private final NotificationType notificationType;
    private final Recipient recipient;
    private final NotificationContent notificationContent;
    private final SenderInfo senderInfo;
    private final Instant scheduledAt;
    private DeliveryStatus deliveryStatus;
    private Instant dispatchedAt;
    private String failureReason;
    private final Instant createdAt;

    /**
     * NotificationMessage 생성자입니다.
     *
     * @param messageId           알림 메시지 ID
     * @param requestId           요청 ID
     * @param notificationType    알림 채널 (SMS, EMAIL, PUSH 등)
     * @param recipient           수신자 정보
     * @param notificationContent 알림 내용
     * @param senderInfo          발신자 정보
     * @param deliveryStatus      발송 상태
     * @param scheduledAt         알림 발송 예정 시간
     * @param dispatchedAt        실제 발송 시스템으로 전달된 시간
     * @param failureReason       실패 사유
     * @param createdAt           생성 시간
     */
    public NotificationMessage(NotificationMessageId messageId, NotificationRequestId requestId,
            NotificationType notificationType, Recipient recipient, NotificationContent content,
            SenderInfo senderInfo, DeliveryStatus deliveryStatus, Instant scheduledAt, Instant dispatchedAt,
            String failureReason, Instant createdAt) {

        try {
            this.messageId = Objects.requireNonNull(messageId, "Notification message ID cannot be null");
            this.requestId = Objects.requireNonNull(requestId, "Notification request ID cannot be null");
            this.notificationType = Objects.requireNonNull(notificationType, "Notification type cannot be null");
            this.recipient = Objects.requireNonNull(recipient, "Recipient cannot be null");
            this.notificationContent = Objects.requireNonNull(content, "Notification content cannot be null");
            this.senderInfo = Objects.requireNonNull(senderInfo, "Sender info cannot be null");
            this.deliveryStatus = Objects.requireNonNull(deliveryStatus, "Delivery status cannot be null");
            this.deliveryStatus = deliveryStatus;
            this.scheduledAt = scheduledAt; // 스케줄링 시간은 현재 시각으로 초기화
            this.dispatchedAt = dispatchedAt; // 발송 시스템에 전달된 시간
            this.failureReason = failureReason; // 실패 사유는 null일 수 있음
            this.createdAt = createdAt;
        } catch (NullPointerException e) {
            throw new MandatoryFieldException(e.getMessage());
        }
    }

    /**
     * NotificationMessage를 생성합니다.
     * 이 메서드는 알림 요청이 생성될 때 호출됩니다.
     *
     * @param requestId           요청 ID
     * @param notificationType    알림 채널 (SMS, EMAIL, PUSH 등)
     * @param recipient           수신자 정보
     * @param notificationContent 알림 내용
     * @param senderInfo          발신자 정보
     * @param scheduledAt         알림 발송 예정 시간
     * @return 생성된 NotificationMessage 객체
     */
    public static NotificationMessage create(NotificationRequestId requestId, NotificationType notificationType,
            Recipient recipient, NotificationContent notificationContent,
            SenderInfo senderInfo, Instant scheduledAt) {
        return new NotificationMessage(NotificationMessageId.create(), requestId,
                notificationType, recipient, notificationContent,
                senderInfo, DeliveryStatus.PENDING, scheduledAt,
                null, null, null);
    }

    /**
     * 알림 메시지를 DISPATCHED 상태로 변경합니다.
     * 이 메서드는 알림이 발송 시스템으로 전달되었을 때 호출됩니다.
     */
    public void markAsDispatched() {
        if (this.deliveryStatus != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Cannot mark as dispatched when status is not PENDING");
        }

        this.deliveryStatus = DeliveryStatus.DISPATCHED;
        this.dispatchedAt = Instant.now();
    }

    /**
     * 알림 메시지를 FAILED 상태로 변경합니다.
     * 이 메서드는 알림 발송이 실패했을 때 호출됩니다.
     *
     * @param reason 실패 사유
     */
    public void markAsFailed(String reason) {
        if (this.deliveryStatus != DeliveryStatus.PENDING && this.deliveryStatus != DeliveryStatus.DISPATCHED) {
            throw new IllegalStateException("Cannot mark as failed when status is not PENDING or DISPATCHED");
        }

        this.deliveryStatus = DeliveryStatus.FAILED;
        this.failureReason = reason;
        this.dispatchedAt = Instant.now(); // 실패 시 현재 시각으로 설정
    }

}
