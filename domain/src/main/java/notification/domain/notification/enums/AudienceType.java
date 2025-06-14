package notification.domain.notification.enums;

public enum AudienceType {
    INDIVIDUAL, // 1:1 사용자 (예: 특정 userId)
    GROUP, // 그룹 (예: 채팅방 ID)
    BROADCAST // 전체 사용자 (예: 모든 활성 사용자, 특정 세그먼트)
}
