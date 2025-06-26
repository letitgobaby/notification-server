package notification.definition.enums;

public enum DeliveryStatus {
    PENDING, // 발송 대기 중 (스케줄링 대기 또는 발송기 전달 대기)
    DISPATCHED, // 발송 시스템으로 전송됨 (실제 수신자에게 도달 여부 미확인)
    FAILED; // 발송 또는 전달 실패
}
