package notification.definition.enums;

public enum OutboxStatus {
    PENDING, // 전송 대기
    SENT, // 전송 성공 (Outbox 메시지 삭제)
    DEAD, // 영구 실패 (재시도하지 않음)
    FAILED; // 전송 실패 (재시도 가능)
}