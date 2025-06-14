package notification.domain.notification.enums;

public enum NotificationStatus {
    REGISTERED, // 알림 요청이 접수됨
    SCHEDULED, // 예약 발송 대기 중
    PENDING_PUBLISH, // 즉시 발송을 위해 아웃박스 발행 대기 중
    PUBLISHED, // 아웃박스를 통해 이벤트 발행됨 (발송 전 단계)
    DELIVERED, // 성공적으로 발송됨
    FAILED_DELIVERY, // 발송 실패
    CANCELLED // 알림 요청 취소됨
}
