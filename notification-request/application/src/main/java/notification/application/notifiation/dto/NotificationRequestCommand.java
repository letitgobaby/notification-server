package notification.application.notifiation.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import notification.domain.enums.NotificationType;
import notification.domain.enums.RequesterType;

/**
 * 컨트롤러에서 전달받는 알림 요청 Command 객체
 * JSON 요청 구조를 그대로 반영한 DTO
 */
public record NotificationRequestCommand(
        RequesterCommand requester,
        RecipientsCommand recipients,
        List<NotificationType> notificationTypes,
        Map<NotificationType, SenderInfoCommand> senderInfos,
        ContentCommand content, // 직접 작성한 내용 (템플릿 미사용 시)
        TemplateCommand template, // 템플릿 정보 (템플릿 사용 시)
        Instant scheduledAt,
        String memo) {

    public NotificationRequestCommand {
        Objects.requireNonNull(requester, "Requester cannot be null");
        Objects.requireNonNull(recipients, "Recipients cannot be null");
        Objects.requireNonNull(notificationTypes, "Notification types cannot be null");
        Objects.requireNonNull(senderInfos, "Sender infos cannot be null");

        if (notificationTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one notification type is required");
        }

        // content와 template 중 하나만 있어야 함
        if (content != null && template != null) {
            throw new IllegalArgumentException("Cannot provide both content and template");
        }
        if (content == null && template == null) {
            throw new IllegalArgumentException("Either content or template must be provided");
        }
    }

    /**
     * 요청자 정보
     */
    public record RequesterCommand(
            RequesterType type,
            String id) {
        public RequesterCommand {
            Objects.requireNonNull(type, "Requester type cannot be null");
            Objects.requireNonNull(id, "Requester id cannot be null");
        }
    }

    /**
     * 수신자 정보
     */
    public record RecipientsCommand(
            List<String> userIds, // 회원 사용자 ID 리스트
            List<DirectRecipientCommand> directRecipients, // 비회원 직접 수신자
            String segment, // 세그먼트 (예: "LOYAL_CUSTOMERS_PURCHASE_OVER_1M")
            Boolean allUsers // 전체 사용자 대상 여부
    ) {
        public RecipientsCommand {
            // 적어도 하나의 수신자 정보가 있어야 함
            boolean hasUserIds = userIds != null && !userIds.isEmpty();
            boolean hasDirectRecipients = directRecipients != null && !directRecipients.isEmpty();
            boolean hasSegment = segment != null && !segment.isBlank();
            boolean hasAllUsers = allUsers != null && allUsers;

            if (!hasUserIds && !hasDirectRecipients && !hasSegment && !hasAllUsers) {
                throw new IllegalArgumentException("At least one recipient type must be specified");
            }
        }
    }

    /**
     * 직접 수신자 정보 (비회원)
     */
    public record DirectRecipientCommand(
            String phoneNumber,
            String email,
            String deviceToken) {
        public DirectRecipientCommand {
            // 적어도 하나의 연락처 정보가 있어야 함
            if ((phoneNumber == null || phoneNumber.isBlank()) &&
                    (email == null || email.isBlank()) &&
                    (deviceToken == null || deviceToken.isBlank())) {
                throw new IllegalArgumentException("At least one contact method must be provided");
            }
        }
    }

    /**
     * 발신자 정보 (알림 채널별)
     */
    public record SenderInfoCommand(
            String senderPhoneNumber, // SMS용
            String senderEmailAddress, // EMAIL용
            String senderName // 공통 또는 PUSH용
    ) {
    }

    /**
     * 직접 작성한 알림 내용
     */
    public record ContentCommand(
            String title,
            String body,
            String redirectUrl,
            String imageUrl) {
        public ContentCommand {
            // title과 body 중 적어도 하나는 있어야 함
            if ((title == null || title.isBlank()) && (body == null || body.isBlank())) {
                throw new IllegalArgumentException("At least title or body must be provided");
            }
        }
    }

    /**
     * 템플릿 정보
     */
    public record TemplateCommand(
            String templateId,
            Map<String, String> templateParameters) {
        public TemplateCommand {
            Objects.requireNonNull(templateId, "Template ID cannot be null");
            if (templateId.isBlank()) {
                throw new IllegalArgumentException("Template ID cannot be blank");
            }
        }
    }
}
