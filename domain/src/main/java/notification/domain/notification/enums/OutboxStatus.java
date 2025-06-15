package notification.domain.notification.enums;

public enum OutboxStatus {
    PENDING, // 전송 대기
    SENT, // 전송 성공
    FAILED, // 전송 실패 (재시도 가능)
    DEAD // 영구 실패, 더 이상 재시도하지 않음
}
