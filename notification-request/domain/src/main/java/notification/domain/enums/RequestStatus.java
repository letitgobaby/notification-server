package notification.domain.enums;

public enum RequestStatus {
    PENDING, // 요청 접수됨, 처리 대기 중
    PROCESSING, // 알림 항목 생성 및 스케줄링/발송 준비 중
    DISPATCHED, // 알림 항목이 발송됨
    FAILED, // 요청 처리 중 치명적인 실패 발생
    CANCELED; // 요청 취소됨
}