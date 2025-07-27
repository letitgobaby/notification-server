package notification.adapter.web.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import notification.domain.enums.NotificationType;
import notification.domain.enums.RequesterType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationCreateRequest {

    @NotNull(message = "Requester information is required")
    @Valid
    private RequesterRequest requester;

    @NotNull(message = "Recipient information is required")
    @Valid
    private RecipientsRequest recipients;

    @NotNull(message = "Notification types are required")
    @NotEmpty(message = "At least one notification type must be specified")
    private List<NotificationType> notificationTypes;

    @NotNull(message = "Sender infos are required")
    @NotEmpty(message = "Sender infos cannot be empty")
    private Map<NotificationType, @Valid SenderInfoRequest> senderInfos; // Map 값도 유효성 검증

    @Valid
    private ContentRequest content; // 직접 작성한 내용 (템플릿 미사용 시)

    @Valid
    private TemplateRequest template; // 템플릿 정보 (템플릿 사용 시)

    private Instant scheduledAt; // 예약 시간 (선택 사항)

    private String memo; // 메모 (선택 사항)

    // content와 template 중 하나만 있어야 한다는 커스텀 유효성 검증
    @AssertTrue(message = "Either content or template must be provided, but not both")
    private boolean isContentOrTemplateProvided() {
        return (content != null && template == null) || (content == null && template != null);
    }

    /**
     * 요청자 정보
     * Inner Class for better encapsulation
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequesterRequest {
        @NotNull(message = "Requester type cannot be null")
        private RequesterType type; // RequesterType enum이 도메인에 있다고 가정

        @NotBlank(message = "Requester ID cannot be empty")
        private String id;
    }

    /**
     * 수신자 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecipientsRequest {
        private List<String> userIds; // 회원 사용자 ID 리스트
        @Valid
        private List<DirectRecipientRequest> directRecipients; // 비회원 직접 수신자
        private String segment; // 세그먼트 (예: "LOYAL_CUSTOMERS_PURCHASE_OVER_1M")
        private Boolean allUsers; // 전체 사용자 대상 여부 (Boolean 객체로 null 허용)

        // 적어도 하나의 수신자 정보가 있어야 한다는 커스텀 유효성 검증
        @AssertTrue(message = "At least one recipient type (userIds, directRecipients, segment, or allUsers) must be specified")
        private boolean isAtLeastOneRecipientTypeSpecified() {
            boolean hasUserIds = userIds != null && !userIds.isEmpty();
            boolean hasDirectRecipients = directRecipients != null && !directRecipients.isEmpty();
            boolean hasSegment = segment != null && !segment.isBlank();
            boolean hasAllUsers = allUsers != null && allUsers; // allUsers가 true일 경우만 유효

            return hasUserIds || hasDirectRecipients || hasSegment || hasAllUsers;
        }
    }

    /**
     * 직접 수신자 정보 (비회원)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DirectRecipientRequest {
        private String phoneNumber;
        private String email;
        private String deviceToken;

        // 적어도 하나의 연락처 정보가 있어야 한다는 커스텀 유효성 검증
        @AssertTrue(message = "At least one contact method (phoneNumber, email, or deviceToken) must be provided for direct recipient")
        private boolean isAtLeastOneContactMethodProvided() {
            return (phoneNumber != null && !phoneNumber.isBlank()) ||
                    (email != null && !email.isBlank()) ||
                    (deviceToken != null && !deviceToken.isBlank());
        }
    }

    /**
     * 발신자 정보 (알림 채널별)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SenderInfoRequest {
        private String senderPhoneNumber; // SMS용
        private String senderEmailAddress; // EMAIL용
        private String senderName; // 공통 또는 PUSH용
    }

    /**
     * 직접 작성한 알림 내용
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContentRequest {
        private String title;
        private String body;
        private String redirectUrl;
        private String imageUrl;

        // title과 body 중 적어도 하나는 있어야 한다는 커스텀 유효성 검증
        @AssertTrue(message = "At least title or body must be provided for content")
        private boolean isTitleOrBodyProvided() {
            return (title != null && !title.isBlank()) || (body != null && !body.isBlank());
        }
    }

    /**
     * 템플릿 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateRequest {
        @NotBlank(message = "Template ID cannot be empty")
        private String templateId;
        private Map<String, String> templateParameters;
    }
}
