package notification.domain;

import java.time.Instant;
import java.util.Objects;

import lombok.Getter;
import notification.definition.annotations.AggregateRoot;
import notification.definition.enums.DeliveryStatus;
import notification.definition.enums.NotificationType;
import notification.definition.exceptions.MandatoryFieldException;
import notification.definition.exceptions.PolicyViolationException;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Recipient;
import notification.domain.vo.SenderInfo;

@AggregateRoot
@Getter
public class NotificationMessage {
    private final NotificationMessageId itemId;
    private final NotificationRequestId notificationRequestId; // 어떤 요청에 의해 생성되었는지 참조
    private final NotificationType notificationType;
    private final Recipient recipient;
    private final NotificationContent notificationContent;
    private final SenderInfo senderInfo;
    private DeliveryStatus deliveryStatus;
    private Instant scheduledAt; // 알림 발송 예정 시간 (지연 메시지용)
    private Instant dispatchedAt; // 실제 발송 시스템으로 전달된 시간
    private String failureReason;

    /**
     * NotificationMessage 생성자입니다.
     *
     * @param itemId                알림 항목 ID
     * @param notificationRequestId 요청 ID
     * @param notificationType      알림 채널 (SMS, EMAIL, PUSH 등)
     * @param recipient             수신자 정보
     * @param notificationContent   알림 내용
     * @param senderInfo            발신자 정보
     * @param scheduledAt           알림 발송 예정 시간
     */
    public NotificationMessage(NotificationMessageId itemId, NotificationRequestId notificationRequestId,
            NotificationType notificationType, Recipient recipient, NotificationContent notificationContent,
            SenderInfo senderInfo, DeliveryStatus deliveryStatus, Instant scheduledAt, Instant dispatchedAt,
            String failureReason) {
        try {
            this.itemId = Objects.requireNonNull(itemId, "Notification item ID cannot be null");
            this.notificationRequestId = Objects.requireNonNull(notificationRequestId,
                    "Notification request ID cannot be null");
            this.notificationType = Objects.requireNonNull(notificationType, "Notification type cannot be null");
            this.recipient = Objects.requireNonNull(recipient, "Recipient cannot be null");
            this.notificationContent = Objects.requireNonNull(notificationContent,
                    "Notification content cannot be null");
            this.senderInfo = Objects.requireNonNull(senderInfo, "Sender info cannot be null");
            this.deliveryStatus = Objects.requireNonNull(deliveryStatus, "Delivery status cannot be null");
            this.scheduledAt = scheduledAt; // 스케줄링 시간은 현재 시각으로 초기화
            this.dispatchedAt = dispatchedAt; // 발송 시스템에 전달된 시간
            this.failureReason = failureReason; // 실패 사유는 null일 수 있음
        } catch (NullPointerException e) {
            throw new MandatoryFieldException(e.getMessage(), e);
        }
    }

    /**
     * NotificationMessage 생성 팩토리 메서드입니다.
     *
     * @param notificationRequestId 요청 ID
     * @param notificationType      알림 채널 (SMS, EMAIL, PUSH 등)
     * @param recipient             수신자 정보
     * @param notificationContent   알림 내용
     * @param senderInfo            발신자 정보
     * @param scheduledAt           알림 발송 예정 시간
     * @return 새로운 NotificationMessage 인스턴스
     */
    public static NotificationMessage create(
            NotificationRequestId notificationRequestId, NotificationType notificationType, Recipient recipient,
            NotificationContent notificationContent, SenderInfo senderInfo, Instant scheduledAt) {
        return new NotificationMessage(NotificationMessageId.generate(),
                notificationRequestId, notificationType, recipient,
                notificationContent, senderInfo, DeliveryStatus.PENDING,
                scheduledAt, null, null);
    }

    /**
     * 알림 메시지를 DISPATCHED 상태로 변경합니다.
     * 이 메서드는 알림이 발송 시스템으로 전달되었을 때 호출됩니다.
     */
    public void markAsDispatched(Instant dispatchedAt) {
        // PENDING 상태에서만 DISPATCHED로 변경 가능
        if (this.deliveryStatus != DeliveryStatus.PENDING) {
            throw new PolicyViolationException("Cannot mark as dispatched unless status is PENDING");
        }
        this.deliveryStatus = DeliveryStatus.DISPATCHED;
        this.dispatchedAt = Objects.requireNonNull(dispatchedAt, "Dispatched time cannot be null");
    }

    /**
     * 알림 메시지를 FAILED 상태로 변경합니다.
     * 이 메서드는 알림 발송에 실패했을 때 호출됩니다.
     *
     * @param reason 실패 사유
     */
    public void markAsFailed(String reason) {
        // FAILED 상태로 변경하려면 PENDING 또는 DISPATCHED 상태여야 함
        if (this.deliveryStatus != DeliveryStatus.PENDING && this.deliveryStatus != DeliveryStatus.DISPATCHED) {
            throw new PolicyViolationException("Cannot mark as failed unless status is PENDING or DISPATCHED");
        }
        this.deliveryStatus = DeliveryStatus.FAILED;
        this.failureReason = Objects.requireNonNull(reason, "Failure reason cannot be null");
        this.dispatchedAt = Instant.now(); // 실패 시 현재 시각으로 설정
    }
}
