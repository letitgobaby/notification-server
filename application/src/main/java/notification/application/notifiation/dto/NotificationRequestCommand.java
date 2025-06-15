package notification.application.notifiation.dto;

import java.time.Instant;

import notification.domain.notification.enums.AudienceType;
import notification.domain.notification.enums.NotificationType;
import notification.domain.notification.enums.Priority;
import notification.domain.notification.enums.RequesterType;
import notification.domain.notification.vo.NotificationContent;
import notification.domain.notification.vo.Recipient;
import notification.domain.notification.vo.RequesterId;

public record NotificationRequestCommand(
        NotificationType type, // 알림 유형 (예: SMS, PUSH, EMAIL, CHAT 등)
        RequesterType requesterType, // 요청자 유형 (예: USER, SYSTEM, ADMIN 등)
        RequesterId requesterId, // 요청자 ID (예: 사용자 ID, 시스템 ID 등)
        AudienceType audienceType, // 대상 유형 (예: INDIVIDUAL, GROUP, BROADCAST 등)
        NotificationContent content, // 템플릿 ID, 템플릿 변수, 이미지 URL, 딥링크 등
        Recipient recipient, // 수신자 식별 정보 (예: userIds, chatRoomId, emailAddress, phoneNumber)
        Instant scheduledAt, // 예약 발송 시간 (ISO 8601 형식)
        Priority priority // 우선순위 (예: HIGH, NORMAL, LOW 등)
) {
}
