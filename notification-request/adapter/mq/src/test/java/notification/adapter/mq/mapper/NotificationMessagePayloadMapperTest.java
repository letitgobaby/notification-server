package notification.adapter.mq.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import notification.adapter.mq.payload.EmailMessagePayload;
import notification.adapter.mq.payload.NotificationMessagePayload;
import notification.adapter.mq.payload.PushMessagePayload;
import notification.adapter.mq.payload.SmsMessagePayload;
import notification.domain.NotificationMessage;
import notification.domain.enums.DeliveryStatus;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SmsSender;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationMessagePayloadMapper 테스트")
class NotificationMessagePayloadMapperTest {

    @InjectMocks
    private NotificationMessagePayloadMapper mapper;

    private NotificationMessageId messageId;
    private NotificationRequestId requestId;
    private Instant createdAt;
    private Recipient recipient;
    private NotificationContent notificationContent;
    private Instant scheduledAt;

    @BeforeEach
    void setUp() {
        messageId = NotificationMessageId.create();
        requestId = NotificationRequestId.create();
        createdAt = Instant.now().minusSeconds(60);
        recipient = new Recipient(
                "user123",
                "test@example.com",
                "01012345678",
                "device-token-123",
                "ko");
        notificationContent = new NotificationContent(
                "테스트 제목",
                "테스트 내용",
                "https://example.com/redirect",
                "https://example.com/image.jpg");
        scheduledAt = Instant.now().plusSeconds(3600);
    }

    @DisplayName("EMAIL 타입의 NotificationMessage를 EmailMessagePayload로 변환")
    @Test
    void toPayload_emailMessage_success() {
        // given
        EmailSender emailSender = new EmailSender("sender@example.com", "테스트 발신자");
        NotificationMessage emailMessage = new NotificationMessage(
                messageId, requestId, NotificationType.EMAIL, recipient, notificationContent,
                emailSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        // when
        Mono<NotificationMessagePayload> result = mapper.toPayload(emailMessage);

        // then
        StepVerifier.create(result)
                .assertNext(payload -> {
                    assertInstanceOf(EmailMessagePayload.class, payload);
                    EmailMessagePayload emailPayload = (EmailMessagePayload) payload;

                    assertEquals(messageId.value(), emailPayload.getMessageId());
                    assertEquals(requestId.value(), emailPayload.getRequestId());
                    assertEquals(createdAt, emailPayload.getCreatedAt());
                    assertEquals(notificationContent.getTitle(), emailPayload.getSubject());
                    assertEquals(notificationContent.getBody(), emailPayload.getBody());
                    assertEquals(emailSender.senderEmailAddress(), emailPayload.getSenderEmail());
                    assertEquals(emailSender.senderName(), emailPayload.getSenderName());
                    assertEquals(recipient.email(), emailPayload.getRecipientEmail());
                    assertEquals("EMAIL", emailPayload.getNotificationType());
                })
                .verifyComplete();
    }

    @DisplayName("SMS 타입의 NotificationMessage를 SmsMessagePayload로 변환")
    @Test
    void toPayload_smsMessage_success() {
        // given
        SmsSender smsSender = new SmsSender("01000000000", "테스트 발신자");
        NotificationMessage smsMessage = new NotificationMessage(
                messageId, requestId, NotificationType.SMS, recipient, notificationContent,
                smsSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        // when
        Mono<NotificationMessagePayload> result = mapper.toPayload(smsMessage);

        // then
        StepVerifier.create(result)
                .assertNext(payload -> {
                    assertInstanceOf(SmsMessagePayload.class, payload);
                    SmsMessagePayload smsPayload = (SmsMessagePayload) payload;

                    assertEquals(messageId.value(), smsPayload.getMessageId());
                    assertEquals(requestId.value(), smsPayload.getRequestId());
                    assertEquals(createdAt, smsPayload.getCreatedAt());
                    assertEquals(smsSender.senderPhoneNumber(), smsPayload.getSenderPhone());
                    assertEquals(recipient.phoneNumber(), smsPayload.getRecipientPhone());
                    assertEquals(notificationContent.getBody(), smsPayload.getMessageText());
                    assertEquals("SMS", smsPayload.getNotificationType());
                })
                .verifyComplete();
    }

    @DisplayName("PUSH 타입의 NotificationMessage를 PushMessagePayload로 변환")
    @Test
    void toPayload_pushMessage_success() {
        // given
        PushSender pushSender = new PushSender("테스트 앱");
        NotificationMessage pushMessage = new NotificationMessage(
                messageId, requestId, NotificationType.PUSH, recipient, notificationContent,
                pushSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        // when
        Mono<NotificationMessagePayload> result = mapper.toPayload(pushMessage);

        // then
        StepVerifier.create(result)
                .assertNext(payload -> {
                    assertInstanceOf(PushMessagePayload.class, payload);
                    PushMessagePayload pushPayload = (PushMessagePayload) payload;

                    assertEquals(messageId.value(), pushPayload.getMessageId());
                    assertEquals(requestId.value(), pushPayload.getRequestId());
                    assertEquals(createdAt, pushPayload.getCreatedAt());
                    assertEquals(recipient.deviceToken(), pushPayload.getDeviceToken());
                    assertEquals(notificationContent.getTitle(), pushPayload.getTitle());
                    assertEquals(notificationContent.getBody(), pushPayload.getBody());
                    assertEquals(notificationContent.getImageUrl(), pushPayload.getImageUrl());
                    assertEquals(notificationContent.getRedirectUrl(),
                            pushPayload.getRedirectUrl());
                    assertEquals(pushSender.senderName(), pushPayload.getSenderName());
                    assertEquals("PUSH", pushPayload.getNotificationType());
                })
                .verifyComplete();
    }

    @DisplayName("null 값이 전달되면 빈 Mono 반환")
    @Test
    void toPayload_nullMessage_returnsEmptyMono() {
        // when
        Mono<NotificationMessagePayload> result = mapper.toPayload(null);

        // then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @DisplayName("null NotificationType인 경우 NullPointerException 발생")
    @Test
    void toPayload_nullNotificationType_throwsNullPointerException() {
        // given
        NotificationMessage message = mock(NotificationMessage.class);
        when(message.getNotificationType()).thenReturn(null); // null 타입 시뮬레이션

        // when
        Mono<NotificationMessagePayload> result = mapper.toPayload(message);

        // then
        StepVerifier.create(result)
                .expectError(NullPointerException.class)
                .verify();
    }

    @DisplayName("EMAIL 페이로드 변환 시 필드 매핑 확인")
    @Test
    void toEmailPayload_fieldMapping_verification() {
        // given
        EmailSender emailSender = new EmailSender("sender-id", "noreply@example.com", "시스템 관리자");
        NotificationMessage emailMessage = new NotificationMessage(
                messageId, requestId, NotificationType.EMAIL, recipient, notificationContent,
                emailSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        // when
        Mono<NotificationMessagePayload> result = mapper.toPayload(emailMessage);

        // then
        StepVerifier.create(result)
                .assertNext(payload -> {
                    EmailMessagePayload emailPayload = (EmailMessagePayload) payload;

                    // 모든 필드가 올바르게 매핑되었는지 확인
                    assertNotNull(emailPayload.getMessageId());
                    assertNotNull(emailPayload.getRequestId());
                    assertNotNull(emailPayload.getCreatedAt());
                    assertNotNull(emailPayload.getSubject());
                    assertNotNull(emailPayload.getBody());
                    assertNotNull(emailPayload.getSenderEmail());
                    assertNotNull(emailPayload.getSenderName());
                    assertNotNull(emailPayload.getRecipientEmail());

                    // 특정 값 검증
                    assertEquals("시스템 관리자", emailPayload.getSenderName());
                    assertEquals("noreply@example.com", emailPayload.getSenderEmail());
                })
                .verifyComplete();
    }

    @DisplayName("SMS 페이로드 변환 시 필드 매핑 확인")
    @Test
    void toSmsPayload_fieldMapping_verification() {
        // given
        SmsSender smsSender = new SmsSender("sender-id", "02-1234-5678", "고객센터");
        NotificationMessage smsMessage = new NotificationMessage(
                messageId, requestId, NotificationType.SMS, recipient, notificationContent,
                smsSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        // when
        Mono<NotificationMessagePayload> result = mapper.toPayload(smsMessage);

        // then
        StepVerifier.create(result)
                .assertNext(payload -> {
                    SmsMessagePayload smsPayload = (SmsMessagePayload) payload;

                    // 모든 필드가 올바르게 매핑되었는지 확인
                    assertNotNull(smsPayload.getMessageId());
                    assertNotNull(smsPayload.getRequestId());
                    assertNotNull(smsPayload.getCreatedAt());
                    assertNotNull(smsPayload.getSenderPhone());
                    assertNotNull(smsPayload.getRecipientPhone());
                    assertNotNull(smsPayload.getMessageText());

                    // 특정 값 검증
                    assertEquals("02-1234-5678", smsPayload.getSenderPhone());
                    assertEquals("01012345678", smsPayload.getRecipientPhone());
                    assertEquals("테스트 내용", smsPayload.getMessageText());
                })
                .verifyComplete();
    }

    @DisplayName("PUSH 페이로드 변환 시 필드 매핑 확인")
    @Test
    void toPushPayload_fieldMapping_verification() {
        // given
        PushSender pushSender = new PushSender("push-sender-id", "모바일 앱");
        NotificationMessage pushMessage = new NotificationMessage(
                messageId, requestId, NotificationType.PUSH, recipient, notificationContent,
                pushSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        // when
        Mono<NotificationMessagePayload> result = mapper.toPayload(pushMessage);

        // then
        StepVerifier.create(result)
                .assertNext(payload -> {
                    PushMessagePayload pushPayload = (PushMessagePayload) payload;

                    // 모든 필드가 올바르게 매핑되었는지 확인
                    assertNotNull(pushPayload.getMessageId());
                    assertNotNull(pushPayload.getRequestId());
                    assertNotNull(pushPayload.getCreatedAt());
                    assertNotNull(pushPayload.getDeviceToken());
                    assertNotNull(pushPayload.getTitle());
                    assertNotNull(pushPayload.getBody());
                    assertNotNull(pushPayload.getImageUrl());
                    assertNotNull(pushPayload.getRedirectUrl());
                    assertNotNull(pushPayload.getSenderName());

                    // 특정 값 검증
                    assertEquals("device-token-123", pushPayload.getDeviceToken());
                    assertEquals("테스트 제목", pushPayload.getTitle());
                    assertEquals("https://example.com/image.jpg", pushPayload.getImageUrl());
                    assertEquals("https://example.com/redirect", pushPayload.getRedirectUrl());
                    assertEquals("모바일 앱", pushPayload.getSenderName());
                })
                .verifyComplete();
    }

    @DisplayName("여러 타입의 메시지를 연속으로 변환")
    @Test
    void toPayload_multipleTypes_success() {
        // given
        EmailSender emailSender = new EmailSender("email@example.com", "이메일 발신자");
        SmsSender smsSender = new SmsSender("01011111111", "SMS 발신자");
        PushSender pushSender = new PushSender("푸시 발신자");

        NotificationMessage emailMessage = new NotificationMessage(
                messageId, requestId, NotificationType.EMAIL, recipient, notificationContent,
                emailSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        NotificationMessage smsMessage = new NotificationMessage(
                NotificationMessageId.create(), requestId, NotificationType.SMS, recipient,
                notificationContent,
                smsSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        NotificationMessage pushMessage = new NotificationMessage(
                NotificationMessageId.create(), requestId, NotificationType.PUSH, recipient,
                notificationContent,
                pushSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        // when & then
        StepVerifier.create(mapper.toPayload(emailMessage))
                .assertNext(payload -> assertInstanceOf(EmailMessagePayload.class, payload))
                .verifyComplete();

        StepVerifier.create(mapper.toPayload(smsMessage))
                .assertNext(payload -> assertInstanceOf(SmsMessagePayload.class, payload))
                .verifyComplete();

        StepVerifier.create(mapper.toPayload(pushMessage))
                .assertNext(payload -> assertInstanceOf(PushMessagePayload.class, payload))
                .verifyComplete();
    }
}
