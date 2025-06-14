package notification.domain.notification.enums;

public enum OutboxStatus {
    SENT, // 메시지가 성공적으로 전송됨
    PENDING, // 메시지가 대기 중 (아직 전송되지 않음)
    FAILED // 메시지 전송에 실패함
}
