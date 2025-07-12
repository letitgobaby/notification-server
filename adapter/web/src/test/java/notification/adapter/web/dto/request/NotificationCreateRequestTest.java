package notification.adapter.web.dto.request;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import notification.domain.enums.NotificationType;
import notification.domain.enums.RequesterType;

@DisplayName("NotificationCreateRequest 테스트")
class NotificationCreateRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("전체 요청 유효성 검증")
    class FullRequestValidation {

        @Test
        @DisplayName("유효한 요청 객체 생성 성공")
        void createValidRequest_success() {
            // given
            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(createValidRecipientsWithUserIds())
                    .notificationTypes(List.of(NotificationType.PUSH))
                    .senderInfos(Map.of(NotificationType.PUSH, createValidSenderInfo()))
                    .content(createValidContent())
                    .scheduledAt(Instant.now().plusSeconds(3600))
                    .memo("Test memo")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertTrue(violations.isEmpty(), "Valid request should have no violations");
        }

        @Test
        @DisplayName("필수 필드 누락 시 유효성 검증 실패")
        void createRequestWithMissingRequiredFields_fails() {
            // given
            NotificationCreateRequest request = NotificationCreateRequest.builder().build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Requester information is required")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Recipient information is required")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Notification types are required")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Sender infos are required")));
        }

        @Test
        @DisplayName("content와 template 둘 다 제공 시 유효성 검증 실패")
        void createRequestWithBothContentAndTemplate_fails() {
            // given
            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(createValidRecipientsWithUserIds())
                    .notificationTypes(List.of(NotificationType.PUSH))
                    .senderInfos(Map.of(NotificationType.PUSH, createValidSenderInfo()))
                    .content(createValidContent())
                    .template(createValidTemplate())
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(
                    v -> v.getMessage().contains("Either content or template must be provided, but not both")));
        }

        @Test
        @DisplayName("content와 template 둘 다 누락 시 유효성 검증 실패")
        void createRequestWithoutContentAndTemplate_fails() {
            // given
            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(createValidRecipientsWithUserIds())
                    .notificationTypes(List.of(NotificationType.PUSH))
                    .senderInfos(Map.of(NotificationType.PUSH, createValidSenderInfo()))
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(
                    v -> v.getMessage().contains("Either content or template must be provided, but not both")));
        }

        @Test
        @DisplayName("빈 notificationTypes 리스트 시 유효성 검증 실패")
        void createRequestWithEmptyNotificationTypes_fails() {
            // given
            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(createValidRecipientsWithUserIds())
                    .notificationTypes(List.of())
                    .senderInfos(Map.of(NotificationType.PUSH, createValidSenderInfo()))
                    .content(createValidContent())
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("At least one notification type must be specified")));
        }
    }

    @Nested
    @DisplayName("RequesterRequest 유효성 검증")
    class RequesterRequestValidation {

        @Test
        @DisplayName("유효한 RequesterRequest 생성 성공")
        void createValidRequesterRequest_success() {
            // given
            NotificationCreateRequest.RequesterRequest requester = createValidRequester();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.RequesterRequest>> violations = validator
                    .validate(requester);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("RequesterType이 null인 경우 유효성 검증 실패")
        void createRequesterRequestWithNullType_fails() {
            // given
            NotificationCreateRequest.RequesterRequest requester = NotificationCreateRequest.RequesterRequest.builder()
                    .type(null)
                    .id("user-123")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.RequesterRequest>> violations = validator
                    .validate(requester);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Requester type cannot be null")));
        }

        @Test
        @DisplayName("빈 ID인 경우 유효성 검증 실패")
        void createRequesterRequestWithBlankId_fails() {
            // given
            NotificationCreateRequest.RequesterRequest requester = NotificationCreateRequest.RequesterRequest.builder()
                    .type(RequesterType.USER)
                    .id("")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.RequesterRequest>> violations = validator
                    .validate(requester);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Requester ID cannot be empty")));
        }
    }

    @Nested
    @DisplayName("RecipientsRequest 유효성 검증")
    class RecipientsRequestValidation {

        @Test
        @DisplayName("userIds만 제공한 경우 유효성 검증 성공")
        void createRecipientsRequestWithUserIds_success() {
            // given
            NotificationCreateRequest.RecipientsRequest recipients = createValidRecipientsWithUserIds();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.RecipientsRequest>> violations = validator
                    .validate(recipients);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("directRecipients만 제공한 경우 유효성 검증 성공")
        void createRecipientsRequestWithDirectRecipients_success() {
            // given
            NotificationCreateRequest.RecipientsRequest recipients = NotificationCreateRequest.RecipientsRequest
                    .builder()
                    .directRecipients(List.of(createValidDirectRecipient()))
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.RecipientsRequest>> violations = validator
                    .validate(recipients);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("segment만 제공한 경우 유효성 검증 성공")
        void createRecipientsRequestWithSegment_success() {
            // given
            NotificationCreateRequest.RecipientsRequest recipients = NotificationCreateRequest.RecipientsRequest
                    .builder()
                    .segment("LOYAL_CUSTOMERS")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.RecipientsRequest>> violations = validator
                    .validate(recipients);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("allUsers만 제공한 경우 유효성 검증 성공")
        void createRecipientsRequestWithAllUsers_success() {
            // given
            NotificationCreateRequest.RecipientsRequest recipients = NotificationCreateRequest.RecipientsRequest
                    .builder()
                    .allUsers(true)
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.RecipientsRequest>> violations = validator
                    .validate(recipients);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("모든 수신자 정보가 없는 경우 유효성 검증 실패")
        void createRecipientsRequestWithoutAnyRecipients_fails() {
            // given
            NotificationCreateRequest.RecipientsRequest recipients = NotificationCreateRequest.RecipientsRequest
                    .builder()
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.RecipientsRequest>> violations = validator
                    .validate(recipients);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("At least one recipient type")));
        }

        @Test
        @DisplayName("allUsers가 false인 경우 유효성 검증 실패")
        void createRecipientsRequestWithFalseAllUsers_fails() {
            // given
            NotificationCreateRequest.RecipientsRequest recipients = NotificationCreateRequest.RecipientsRequest
                    .builder()
                    .allUsers(false)
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.RecipientsRequest>> violations = validator
                    .validate(recipients);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("At least one recipient type")));
        }
    }

    @Nested
    @DisplayName("DirectRecipientRequest 유효성 검증")
    class DirectRecipientRequestValidation {

        @Test
        @DisplayName("phoneNumber만 제공한 경우 유효성 검증 성공")
        void createDirectRecipientRequestWithPhoneNumber_success() {
            // given
            NotificationCreateRequest.DirectRecipientRequest recipient = NotificationCreateRequest.DirectRecipientRequest
                    .builder()
                    .phoneNumber("01012345678")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.DirectRecipientRequest>> violations = validator
                    .validate(recipient);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("email만 제공한 경우 유효성 검증 성공")
        void createDirectRecipientRequestWithEmail_success() {
            // given
            NotificationCreateRequest.DirectRecipientRequest recipient = NotificationCreateRequest.DirectRecipientRequest
                    .builder()
                    .email("test@example.com")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.DirectRecipientRequest>> violations = validator
                    .validate(recipient);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("deviceToken만 제공한 경우 유효성 검증 성공")
        void createDirectRecipientRequestWithDeviceToken_success() {
            // given
            NotificationCreateRequest.DirectRecipientRequest recipient = NotificationCreateRequest.DirectRecipientRequest
                    .builder()
                    .deviceToken("device-token-123")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.DirectRecipientRequest>> violations = validator
                    .validate(recipient);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("모든 연락처 정보가 없는 경우 유효성 검증 실패")
        void createDirectRecipientRequestWithoutAnyContact_fails() {
            // given
            NotificationCreateRequest.DirectRecipientRequest recipient = NotificationCreateRequest.DirectRecipientRequest
                    .builder()
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.DirectRecipientRequest>> violations = validator
                    .validate(recipient);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("At least one contact method")));
        }

        @Test
        @DisplayName("빈 문자열 연락처 정보인 경우 유효성 검증 실패")
        void createDirectRecipientRequestWithBlankContact_fails() {
            // given
            NotificationCreateRequest.DirectRecipientRequest recipient = NotificationCreateRequest.DirectRecipientRequest
                    .builder()
                    .phoneNumber("")
                    .email("   ")
                    .deviceToken("")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.DirectRecipientRequest>> violations = validator
                    .validate(recipient);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("At least one contact method")));
        }
    }

    @Nested
    @DisplayName("ContentRequest 유효성 검증")
    class ContentRequestValidation {

        @Test
        @DisplayName("title만 제공한 경우 유효성 검증 성공")
        void createContentRequestWithTitle_success() {
            // given
            NotificationCreateRequest.ContentRequest content = NotificationCreateRequest.ContentRequest.builder()
                    .title("Test Title")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.ContentRequest>> violations = validator.validate(content);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("body만 제공한 경우 유효성 검증 성공")
        void createContentRequestWithBody_success() {
            // given
            NotificationCreateRequest.ContentRequest content = NotificationCreateRequest.ContentRequest.builder()
                    .body("Test Body")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.ContentRequest>> violations = validator.validate(content);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("title과 body 모두 없는 경우 유효성 검증 실패")
        void createContentRequestWithoutTitleAndBody_fails() {
            // given
            NotificationCreateRequest.ContentRequest content = NotificationCreateRequest.ContentRequest.builder()
                    .redirectUrl("https://example.com")
                    .imageUrl("https://example.com/image.jpg")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.ContentRequest>> violations = validator.validate(content);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("At least title or body must be provided")));
        }
    }

    @Nested
    @DisplayName("TemplateRequest 유효성 검증")
    class TemplateRequestValidation {

        @Test
        @DisplayName("유효한 TemplateRequest 생성 성공")
        void createValidTemplateRequest_success() {
            // given
            NotificationCreateRequest.TemplateRequest template = createValidTemplate();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.TemplateRequest>> violations = validator
                    .validate(template);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("빈 templateId인 경우 유효성 검증 실패")
        void createTemplateRequestWithBlankTemplateId_fails() {
            // given
            NotificationCreateRequest.TemplateRequest template = NotificationCreateRequest.TemplateRequest.builder()
                    .templateId("")
                    .templateParameters(Map.of("key", "value"))
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest.TemplateRequest>> violations = validator
                    .validate(template);

            // then
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Template ID cannot be empty")));
        }
    }

    @Nested
    @DisplayName("Builder 패턴 테스트")
    class BuilderPatternTest {

        @Test
        @DisplayName("Builder를 통한 객체 생성 성공")
        void createRequestUsingBuilder_success() {
            // when
            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(createValidRecipientsWithUserIds())
                    .notificationTypes(List.of(NotificationType.PUSH, NotificationType.EMAIL))
                    .senderInfos(Map.of(
                            NotificationType.PUSH, createValidSenderInfo(),
                            NotificationType.EMAIL, NotificationCreateRequest.SenderInfoRequest.builder()
                                    .senderEmailAddress("sender@example.com")
                                    .senderName("Test Sender")
                                    .build()))
                    .content(createValidContent())
                    .scheduledAt(Instant.now().plusSeconds(3600))
                    .memo("Test memo")
                    .build();

            // then
            assertNotNull(request);
            assertEquals(RequesterType.USER, request.getRequester().getType());
            assertEquals("user-123", request.getRequester().getId());
            assertEquals(2, request.getNotificationTypes().size());
            assertTrue(request.getNotificationTypes().contains(NotificationType.PUSH));
            assertTrue(request.getNotificationTypes().contains(NotificationType.EMAIL));
            assertEquals("Test memo", request.getMemo());
            assertNotNull(request.getScheduledAt());
        }

        @Test
        @DisplayName("Inner class Builder를 통한 객체 생성 성공")
        void createInnerClassObjectsUsingBuilder_success() {
            // when
            NotificationCreateRequest.RequesterRequest requester = NotificationCreateRequest.RequesterRequest.builder()
                    .type(RequesterType.ADMIN)
                    .id("admin-456")
                    .build();

            NotificationCreateRequest.RecipientsRequest recipients = NotificationCreateRequest.RecipientsRequest
                    .builder()
                    .userIds(List.of("user-1", "user-2"))
                    .segment("VIP_USERS")
                    .build();

            // then
            assertNotNull(requester);
            assertEquals(RequesterType.ADMIN, requester.getType());
            assertEquals("admin-456", requester.getId());

            assertNotNull(recipients);
            assertEquals(2, recipients.getUserIds().size());
            assertEquals("VIP_USERS", recipients.getSegment());
        }
    }

    @Nested
    @DisplayName("복합 시나리오 테스트")
    class ComplexScenarioTest {

        @Test
        @DisplayName("여러 수신자 타입이 동시에 제공된 경우")
        void createRequestWithMultipleRecipientTypes_success() {
            // given
            NotificationCreateRequest.RecipientsRequest recipients = NotificationCreateRequest.RecipientsRequest
                    .builder()
                    .userIds(List.of("user-1", "user-2"))
                    .directRecipients(List.of(createValidDirectRecipient()))
                    .segment("LOYAL_CUSTOMERS")
                    .build();

            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(recipients)
                    .notificationTypes(List.of(NotificationType.PUSH))
                    .senderInfos(Map.of(NotificationType.PUSH, createValidSenderInfo()))
                    .content(createValidContent())
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("여러 알림 타입과 각각의 발신자 정보 제공")
        void createRequestWithMultipleNotificationTypes_success() {
            // given
            Map<NotificationType, NotificationCreateRequest.SenderInfoRequest> senderInfos = Map.of(
                    NotificationType.SMS, NotificationCreateRequest.SenderInfoRequest.builder()
                            .senderPhoneNumber("02-1234-5678")
                            .senderName("Test SMS")
                            .build(),
                    NotificationType.EMAIL, NotificationCreateRequest.SenderInfoRequest.builder()
                            .senderEmailAddress("sender@example.com")
                            .senderName("Test Email")
                            .build(),
                    NotificationType.PUSH, NotificationCreateRequest.SenderInfoRequest.builder()
                            .senderName("Test Push")
                            .build());

            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(createValidRecipientsWithUserIds())
                    .notificationTypes(List.of(NotificationType.SMS, NotificationType.EMAIL, NotificationType.PUSH))
                    .senderInfos(senderInfos)
                    .content(createValidContent())
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("템플릿 사용 시 파라미터 포함")
        void createRequestWithTemplateAndParameters_success() {
            // given
            NotificationCreateRequest.TemplateRequest template = NotificationCreateRequest.TemplateRequest.builder()
                    .templateId("WELCOME_TEMPLATE")
                    .templateParameters(Map.of(
                            "userName", "John Doe",
                            "productName", "Smart Watch",
                            "discountRate", "20"))
                    .build();

            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(createValidRecipientsWithUserIds())
                    .notificationTypes(List.of(NotificationType.PUSH))
                    .senderInfos(Map.of(NotificationType.PUSH, createValidSenderInfo()))
                    .template(template)
                    .scheduledAt(Instant.now().plusSeconds(7200))
                    .memo("Welcome template test")
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertTrue(violations.isEmpty());
            assertEquals("WELCOME_TEMPLATE", request.getTemplate().getTemplateId());
            assertEquals(3, request.getTemplate().getTemplateParameters().size());
        }

        @Test
        @DisplayName("즉시 발송 요청 (scheduledAt이 null)")
        void createRequestForImmediateDelivery_success() {
            // given
            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(createValidRecipientsWithUserIds())
                    .notificationTypes(List.of(NotificationType.PUSH))
                    .senderInfos(Map.of(NotificationType.PUSH, createValidSenderInfo()))
                    .content(createValidContent())
                    .scheduledAt(null) // 즉시 발송
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertTrue(violations.isEmpty());
            assertNull(request.getScheduledAt());
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryValueTest {

        @Test
        @DisplayName("최대 길이 문자열 테스트")
        void createRequestWithMaxLengthStrings_success() {
            // given
            String longString = "a".repeat(1000);

            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(createValidRecipientsWithUserIds())
                    .notificationTypes(List.of(NotificationType.PUSH))
                    .senderInfos(Map.of(NotificationType.PUSH, createValidSenderInfo()))
                    .content(NotificationCreateRequest.ContentRequest.builder()
                            .title(longString)
                            .body(longString)
                            .redirectUrl("https://example.com/" + longString)
                            .imageUrl("https://example.com/image/" + longString)
                            .build())
                    .memo(longString)
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("빈 컬렉션과 null 값 처리")
        void createRequestWithEmptyCollectionsAndNulls_mixed() {
            // given
            NotificationCreateRequest.RecipientsRequest recipients = NotificationCreateRequest.RecipientsRequest
                    .builder()
                    .userIds(List.of()) // 빈 리스트
                    .directRecipients(null) // null
                    .segment("VALID_SEGMENT") // 유효한 값
                    .allUsers(null) // null
                    .build();

            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .requester(createValidRequester())
                    .recipients(recipients)
                    .notificationTypes(List.of(NotificationType.PUSH))
                    .senderInfos(Map.of(NotificationType.PUSH, createValidSenderInfo()))
                    .content(createValidContent())
                    .build();

            // when
            Set<ConstraintViolation<NotificationCreateRequest>> violations = validator.validate(request);

            // then
            assertTrue(violations.isEmpty());
        }
    }

    // Helper methods
    private NotificationCreateRequest.RequesterRequest createValidRequester() {
        return NotificationCreateRequest.RequesterRequest.builder()
                .type(RequesterType.USER)
                .id("user-123")
                .build();
    }

    private NotificationCreateRequest.RecipientsRequest createValidRecipientsWithUserIds() {
        return NotificationCreateRequest.RecipientsRequest.builder()
                .userIds(List.of("user-456", "user-789"))
                .build();
    }

    private NotificationCreateRequest.DirectRecipientRequest createValidDirectRecipient() {
        return NotificationCreateRequest.DirectRecipientRequest.builder()
                .phoneNumber("01012345678")
                .email("test@example.com")
                .deviceToken("device-token-123")
                .build();
    }

    private NotificationCreateRequest.SenderInfoRequest createValidSenderInfo() {
        return NotificationCreateRequest.SenderInfoRequest.builder()
                .senderName("TestApp")
                .build();
    }

    private NotificationCreateRequest.ContentRequest createValidContent() {
        return NotificationCreateRequest.ContentRequest.builder()
                .title("Test Title")
                .body("Test Body")
                .redirectUrl("https://example.com")
                .imageUrl("https://example.com/image.jpg")
                .build();
    }

    private NotificationCreateRequest.TemplateRequest createValidTemplate() {
        return NotificationCreateRequest.TemplateRequest.builder()
                .templateId("TEMPLATE_001")
                .templateParameters(Map.of("key", "value"))
                .build();
    }
}
