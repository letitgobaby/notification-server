package notification.definition.enums;

public enum AudienceType {
    SINGLE_USER,
    MULTIPLE_USERS,
    ALL_USERS,
    SEGMENT, // 특정 세그먼트에 속하는 사용자들
    DIRECT_RECIPIENTS, // 직접 지정된 수신자들 (예: 이메일 주소, 전화번호 등)
    MIXED; // 혼합형: 사용자 그룹과 직접 수신자 모두 포함
}
