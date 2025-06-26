
// NotificationRequestReceivedEvent.java
// 알림 요청이 접수되었을 때 발행
public record NotificationRequestReceivedEvent(
    NotificationRequestId requestId,
    boolean useTemplate,
    @Nullable TemplateInfo templateInfo,
    @Nullable NotificationContent directContent,
    Requester requester,
    TargetAudience targetAudience,
    List<NotificationChannelConfig> channelConfigs
) {}

// NotificationRequestStatusUpdatedEvent.java
// NotificationRequest의 상태가 변경되었을 때 발행
public record NotificationRequestStatusUpdatedEvent(
    NotificationRequestId requestId,
    NotificationRequestStatus newStatus,
    @Nullable String failureReason
) {}

// NotificationItemScheduledEvent.java
// NotificationItem이 생성되고 스케줄링되었을 때 (지연 MQ로 보내기 전) 발행
public record NotificationItemScheduledEvent(
    NotificationItemId itemId,
    NotificationRequestId notificationRequestId,
    NotificationType notificationType,
    Recipient recipient,
    NotificationContent notificationContent,
    SenderInfo senderInfo,
    LocalDateTime scheduledAt
) {}

// NotificationItemDispatchedEvent.java
// NotificationItem이 실제 발송 시스템으로 전달되었을 때 발행 (UserNotification 생성 트리거)
public record NotificationItemDispatchedEvent(
    NotificationItemId itemId,
    NotificationRequestId notificationRequestId,
    UserId userId, // 회원인 경우 UserId 포함
    NotificationType notificationType,
    NotificationContent notificationContent,
    SenderInfo senderInfo,
    LocalDateTime dispatchedAt,
    DeliveryStatus currentDeliveryStatus
) {}

// NotificationItemDeliveryStatusUpdatedEvent.java
// NotificationItem의 최종 발송 상태가 업데이트되었을 때 발행 (UserNotification 업데이트 트리거)
public record NotificationItemDeliveryStatusUpdatedEvent(
    NotificationItemId itemId,
    DeliveryStatus newStatus,
    @Nullable String failureReason
) {}

// UserNotificationReadEvent.java
// 사용자가 알림 내역을 읽음 처리했을 때 발행 (선택 사항)
public record UserNotificationReadEvent(
    UserNotificationId userNotificationId,
    UserId userId,
    LocalDateTime readAt
) {}
