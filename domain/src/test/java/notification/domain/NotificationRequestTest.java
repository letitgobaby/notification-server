package notification.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import notification.domain.common.exceptions.DomainValidationException;
import notification.domain.notification.NotificationRequest;
import notification.domain.notification.enums.AudienceType;
import notification.domain.notification.enums.NotificationType;
import notification.domain.notification.enums.Priority;
import notification.domain.notification.enums.RequesterType;
import notification.domain.notification.vo.EmailContent;
import notification.domain.notification.vo.EmailRecipient;
import notification.domain.notification.vo.NotificationContent;
import notification.domain.notification.vo.NotificationId;
import notification.domain.notification.vo.PushContent;
import notification.domain.notification.vo.PushRecipient;
import notification.domain.notification.vo.Recipient;
import notification.domain.notification.vo.RequesterId;
import notification.domain.notification.vo.SmsContent;
import notification.domain.notification.vo.SmsRecipient;

class NotificationRequestTest {

    private NotificationId notificationId() {
        return NotificationId.generate();
    }

    private RequesterId requesterId() {
        return new RequesterId("requester-1");
    }

    private RequesterType requesterType() {
        return RequesterType.USER; // 예: USER, SYSTEM, ADMIN 등
    }

    private AudienceType audienceType() {
        return AudienceType.INDIVIDUAL;
    }

    private Priority priority() {
        return Priority.LOW;
    }

    private Instant now() {
        return Instant.now();
    }

    @Test
    void createSmsNotificationRequest_success() {
        SmsContent content = new SmsContent(null, null, "Hello, SMS!", false);
        SmsRecipient recipient = new SmsRecipient(List.of("01012345678"), null);

        NotificationRequest request = new NotificationRequest(
                notificationId(),
                requesterId(),
                requesterType(),
                NotificationType.SMS,
                audienceType(),
                recipient,
                content,
                null,
                priority(),
                now());

        assertEquals(NotificationType.SMS, request.getType());
        assertEquals(content, request.getContent());
        assertEquals(recipient, request.getRecipien());
    }

    @Test
    void createPushNotificationRequest_success() {
        PushContent content = new PushContent(null, null, "Push title", "Push body", null, null, null);
        PushRecipient recipient = new PushRecipient(List.of("device-token-1"));

        NotificationRequest request = new NotificationRequest(
                notificationId(),
                requesterId(),
                requesterType(),
                NotificationType.PUSH,
                audienceType(),
                recipient,
                content,
                null,
                priority(),
                now());

        assertEquals(NotificationType.PUSH, request.getType());
        assertEquals(content, request.getContent());
        assertEquals(recipient, request.getRecipien());
    }

    @Test
    void createEmailNotificationRequest_success() {
        EmailContent content = new EmailContent(null, null, "Subject", "Body", null, null);
        EmailRecipient recipient = new EmailRecipient(List.of("test@example.com"), null, null);

        NotificationRequest request = new NotificationRequest(
                notificationId(),
                requesterId(),
                requesterType(),
                NotificationType.EMAIL,
                audienceType(),
                recipient,
                content,
                null,
                priority(),
                now());

        assertEquals(NotificationType.EMAIL, request.getType());
        assertEquals(content, request.getContent());
        assertEquals(recipient, request.getRecipien());
    }

    @DisplayName("예약 시각이 과거인 경우 DomainValidationException을 던진다")
    @Test
    void scheduledAtInPast_throwsException() {
        SmsContent content = new SmsContent(null, null, "Hello, SMS!", false);
        SmsRecipient recipient = new SmsRecipient(List.of("01012345678"), null);
        Instant past = Instant.now().minusSeconds(60);

        DomainValidationException ex = assertThrows(DomainValidationException.class,
                () -> new NotificationRequest(
                        notificationId(),
                        requesterId(),
                        requesterType(),
                        NotificationType.SMS,
                        audienceType(),
                        recipient,
                        content,
                        past,
                        priority(),
                        now()));
        assertTrue(ex.getMessage().contains("Scheduled time cannot be in the past"));
    }

    // --- null Arguments를 테스트하기 위한 데이터 소스 메서드 ---
    static Stream<Arguments> nullArgumentsForNotificationRequestConstructor() {
        // 유효한 기본 값들
        NotificationId baseNotificationId = NotificationId.generate();
        RequesterId baseRequesterId = new RequesterId("test-requester");
        RequesterType baseRequesterType = RequesterType.USER;
        NotificationType baseType = NotificationType.SMS;
        AudienceType baseAudienceType = AudienceType.INDIVIDUAL;
        SmsContent baseContent = new SmsContent(null, null, "Hello, SMS!", false);
        SmsRecipient baseRecipient = new SmsRecipient(List.of("01012345678"), null);
        Instant baseScheduledAt = null; // null도 유효함
        Priority basePriority = Priority.LOW;
        Instant baseRequestedAt = Instant.now();

        return Stream.of(
                Arguments.of(null, baseRequesterId, baseRequesterType, baseType, baseAudienceType, baseRecipient,
                        baseContent, baseScheduledAt, basePriority, baseRequestedAt),
                Arguments.of(baseNotificationId, null, baseRequesterType, baseType, baseAudienceType, baseRecipient,
                        baseContent, baseScheduledAt, basePriority, baseRequestedAt),
                Arguments.of(baseNotificationId, baseRequesterId, null, baseType, baseAudienceType, baseRecipient,
                        baseContent, baseScheduledAt, basePriority, baseRequestedAt),
                Arguments.of(baseNotificationId, baseRequesterId, baseRequesterType, null, baseAudienceType,
                        baseRecipient, baseContent, baseScheduledAt, basePriority, baseRequestedAt),
                Arguments.of(baseNotificationId, baseRequesterId, baseRequesterType, baseType, null, baseRecipient,
                        baseContent, baseScheduledAt, basePriority, baseRequestedAt),
                Arguments.of(baseNotificationId, baseRequesterId, baseRequesterType, baseType, baseAudienceType, null,
                        baseContent, baseScheduledAt, basePriority, baseRequestedAt),
                Arguments.of(baseNotificationId, baseRequesterId, baseRequesterType, baseType, baseAudienceType,
                        baseRecipient, null, baseScheduledAt, basePriority, baseRequestedAt),
                // priority가 null일 때 예외 발생 테스트 추가 (현재 생성자 로직에서 Objects.requireNonNull로 처리)
                Arguments.of(baseNotificationId, baseRequesterId, baseRequesterType, baseType, baseAudienceType,
                        baseRecipient, baseContent, baseScheduledAt, null, baseRequestedAt),
                // requestedAt이 null일 때 예외 발생 테스트 추가 (현재 생성자 로직에서 Objects.requireNonNull로 처리)
                Arguments.of(baseNotificationId, baseRequesterId, baseRequesterType, baseType, baseAudienceType,
                        baseRecipient, baseContent, baseScheduledAt, basePriority, null));
    }

    @DisplayName("필수 인자가 null인 경우 DomainValidationException을 던진다")
    @ParameterizedTest
    @MethodSource("nullArgumentsForNotificationRequestConstructor")
    void constructor_nullArguments_throwDomainValidationException(
            NotificationId notificationId,
            RequesterId requesterId,
            RequesterType requesterType,
            NotificationType type,
            AudienceType audienceType,
            Recipient recipient,
            NotificationContent content,
            Instant scheduledAt,
            Priority priority,
            Instant requestedAt) {

        assertThrows(DomainValidationException.class, () -> new NotificationRequest(
                notificationId,
                requesterId,
                requesterType,
                type,
                audienceType,
                recipient,
                content,
                scheduledAt,
                priority,
                requestedAt));
    }

    // 알림 유형, 콘텐츠 유형, 수신자 유형이 일치하지 않는 경우를 테스트하기 위한 데이터 소스 메서드
    static Stream<Arguments> notificationTypeContentAndRecipientProvider() {
        return Stream.of(
                Arguments.of(NotificationType.SMS,
                        new EmailContent(null, null, "Subject", "Body", null, null),
                        new SmsRecipient(List.of("01012345678"), null)),
                Arguments.of(NotificationType.PUSH,
                        new SmsContent(null, null, "Hello, SMS!", false),
                        new PushRecipient(List.of("device-token-1"))),
                Arguments.of(NotificationType.EMAIL,
                        new PushContent(null, null, "Push title", "Push body", null, null, null),
                        new EmailRecipient(List.of("example@gmail.com"), null, null)));
    }

    @DisplayName("알림 유형과 콘텐츠, 수신자 유형이 일치하지 않는 경우 DomainValidationException을 던진다")
    @ParameterizedTest
    @MethodSource("notificationTypeContentAndRecipientProvider")
    void constructor_inconsistentType_throwDomainValidationException(
            NotificationType type, NotificationContent content, Recipient recipient) {

        DomainValidationException ex = assertThrows(DomainValidationException.class,
                () -> new NotificationRequest(
                        notificationId(),
                        requesterId(),
                        requesterType(),
                        type,
                        audienceType(),
                        recipient,
                        content,
                        null,
                        priority(),
                        now()));

        assertTrue(ex.getMessage().contains("inconsistent"));
    }

}
