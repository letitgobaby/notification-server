package notification.adapter.mq;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import notification.adapter.mq.mapper.NotificationMessagePayloadMapper;
import notification.adapter.mq.payload.EmailMessagePayload;
import notification.adapter.mq.payload.NotificationMessagePayload;
import notification.adapter.mq.payload.PushMessagePayload;
import notification.adapter.mq.payload.SmsMessagePayload;
import notification.adapter.mq.support.KafkaMessagePublishRouter;
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
@DisplayName("NotificationMessagePublish 테스트")
class NotificationMessagePublishTest {

    @Mock
    private NotificationMessagePayloadMapper payloadMapper;

    @Mock
    private KafkaMessagePublishRouter publishRouter;

    @InjectMocks
    private NotificationMessagePublish notificationMessagePublish;

    private NotificationMessage emailMessage;
    private NotificationMessage smsMessage;
    private NotificationMessage pushMessage;
    private EmailMessagePayload emailPayload;
    private SmsMessagePayload smsPayload;
    private PushMessagePayload pushPayload;

    @BeforeEach
    void setUp() {
        NotificationMessageId messageId = NotificationMessageId.create();
        NotificationRequestId requestId = NotificationRequestId.create();
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        Recipient recipient = new Recipient(
                "user123",
                "test@example.com",
                "01012345678",
                "device-token-123",
                "ko");

        NotificationContent content = new NotificationContent(
                "테스트 제목",
                "테스트 내용",
                "https://example.com/redirect",
                "https://example.com/image.jpg");

        // EMAIL 메시지 설정
        EmailSender emailSender = new EmailSender("sender@example.com", "테스트 발신자");
        emailMessage = new NotificationMessage(
                messageId, requestId, NotificationType.EMAIL, recipient, content,
                emailSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        emailPayload = EmailMessagePayload.builder()
                .messageId(messageId.value())
                .requestId(requestId.value())
                .createdAt(createdAt)
                .subject(content.title())
                .body(content.body())
                .senderEmail(emailSender.senderEmailAddress())
                .senderName(emailSender.senderName())
                .recipientEmail(recipient.email())
                .build();

        // SMS 메시지 설정
        SmsSender smsSender = new SmsSender("01000000000", "SMS 발신자");
        smsMessage = new NotificationMessage(
                NotificationMessageId.create(), requestId, NotificationType.SMS, recipient, content,
                smsSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        smsPayload = SmsMessagePayload.builder()
                .messageId(messageId.value())
                .requestId(requestId.value())
                .createdAt(createdAt)
                .senderPhone(smsSender.senderPhoneNumber())
                .recipientPhone(recipient.phoneNumber())
                .messageText(content.body())
                .build();

        // PUSH 메시지 설정
        PushSender pushSender = new PushSender("푸시 발신자");
        pushMessage = new NotificationMessage(
                NotificationMessageId.create(), requestId, NotificationType.PUSH, recipient, content,
                pushSender, DeliveryStatus.PENDING, scheduledAt, null, null, createdAt);

        pushPayload = PushMessagePayload.builder()
                .messageId(messageId.value())
                .requestId(requestId.value())
                .createdAt(createdAt)
                .deviceToken(recipient.deviceToken())
                .title(content.title())
                .body(content.body())
                .imageUrl(content.imageUrl())
                .redirectUrl(content.redirectUrl())
                .senderName(pushSender.senderName())
                .build();
    }

    @DisplayName("EMAIL 메시지 발행 성공")
    @Test
    void publish_emailMessage_success() {
        // given
        when(payloadMapper.toPayload(emailMessage)).thenReturn(Mono.just(emailPayload));
        when(publishRouter.publish(emailPayload, NotificationType.EMAIL)).thenReturn(Mono.empty());

        // when
        Mono<Void> result = notificationMessagePublish.publish(emailMessage);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(payloadMapper).toPayload(emailMessage);
        verify(publishRouter).publish(emailPayload, NotificationType.EMAIL);
    }

    @DisplayName("SMS 메시지 발행 성공")
    @Test
    void publish_smsMessage_success() {
        // given
        when(payloadMapper.toPayload(smsMessage)).thenReturn(Mono.just(smsPayload));
        when(publishRouter.publish(smsPayload, NotificationType.SMS)).thenReturn(Mono.empty());

        // when
        Mono<Void> result = notificationMessagePublish.publish(smsMessage);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(payloadMapper).toPayload(smsMessage);
        verify(publishRouter).publish(smsPayload, NotificationType.SMS);
    }

    @DisplayName("PUSH 메시지 발행 성공")
    @Test
    void publish_pushMessage_success() {
        // given
        when(payloadMapper.toPayload(pushMessage)).thenReturn(Mono.just(pushPayload));
        when(publishRouter.publish(pushPayload, NotificationType.PUSH)).thenReturn(Mono.empty());

        // when
        Mono<Void> result = notificationMessagePublish.publish(pushMessage);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(payloadMapper).toPayload(pushMessage);
        verify(publishRouter).publish(pushPayload, NotificationType.PUSH);
    }

    @DisplayName("PayloadMapper에서 예외 발생 시 예외 전파")
    @Test
    void publish_payloadMapperError_propagatesError() {
        // given
        RuntimeException expectedException = new RuntimeException("Payload mapping failed");
        when(payloadMapper.toPayload(emailMessage)).thenReturn(Mono.error(expectedException));

        // when
        Mono<Void> result = notificationMessagePublish.publish(emailMessage);

        // then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(payloadMapper).toPayload(emailMessage);
        verify(publishRouter, never()).publish(any(NotificationMessagePayload.class),
                any(NotificationType.class));
    }

    @DisplayName("PublishRouter에서 예외 발생 시 예외 전파")
    @Test
    void publish_publishRouterError_propagatesError() {
        // given
        RuntimeException expectedException = new RuntimeException("Publishing failed");
        when(payloadMapper.toPayload(emailMessage)).thenReturn(Mono.just(emailPayload));
        when(publishRouter.publish(emailPayload, NotificationType.EMAIL))
                .thenReturn(Mono.error(expectedException));

        // when
        Mono<Void> result = notificationMessagePublish.publish(emailMessage);

        // then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(payloadMapper).toPayload(emailMessage);
        verify(publishRouter).publish(emailPayload, NotificationType.EMAIL);
    }

    @DisplayName("PayloadMapper가 빈 Mono 반환 시 완료")
    @Test
    void publish_payloadMapperReturnsEmpty_completes() {
        // given
        when(payloadMapper.toPayload(emailMessage)).thenReturn(Mono.empty());

        // when
        Mono<Void> result = notificationMessagePublish.publish(emailMessage);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(payloadMapper).toPayload(emailMessage);
        verify(publishRouter, never()).publish(any(NotificationMessagePayload.class),
                any(NotificationType.class));
    }

    @DisplayName("메시지 타입과 페이로드 타입이 일치하는지 확인")
    @Test
    void publish_messageTypeAndPayloadTypeMatch() {
        // given
        when(payloadMapper.toPayload(any(NotificationMessage.class)))
                .thenReturn(Mono.just(emailPayload))
                .thenReturn(Mono.just(smsPayload))
                .thenReturn(Mono.just(pushPayload));

        when(publishRouter.publish(any(NotificationMessagePayload.class), any(NotificationType.class)))
                .thenReturn(Mono.empty());

        // when & then - EMAIL
        StepVerifier.create(notificationMessagePublish.publish(emailMessage))
                .verifyComplete();

        // when & then - SMS
        StepVerifier.create(notificationMessagePublish.publish(smsMessage))
                .verifyComplete();

        // when & then - PUSH
        StepVerifier.create(notificationMessagePublish.publish(pushMessage))
                .verifyComplete();

        verify(publishRouter).publish(eq(emailPayload), eq(NotificationType.EMAIL));
        verify(publishRouter).publish(eq(smsPayload), eq(NotificationType.SMS));
        verify(publishRouter).publish(eq(pushPayload), eq(NotificationType.PUSH));
    }

    @DisplayName("연속된 메시지 발행")
    @Test
    void publish_multipleMessages_success() {
        // given
        when(payloadMapper.toPayload(emailMessage)).thenReturn(Mono.just(emailPayload));
        when(payloadMapper.toPayload(smsMessage)).thenReturn(Mono.just(smsPayload));
        when(publishRouter.publish(any(NotificationMessagePayload.class), any(NotificationType.class)))
                .thenReturn(Mono.empty());

        // when
        Mono<Void> result1 = notificationMessagePublish.publish(emailMessage);
        Mono<Void> result2 = notificationMessagePublish.publish(smsMessage);

        // then
        StepVerifier.create(result1)
                .verifyComplete();

        StepVerifier.create(result2)
                .verifyComplete();

        verify(payloadMapper).toPayload(emailMessage);
        verify(payloadMapper).toPayload(smsMessage);
        verify(publishRouter).publish(emailPayload, NotificationType.EMAIL);
        verify(publishRouter).publish(smsPayload, NotificationType.SMS);
    }

    @DisplayName("null 메시지 처리")
    @Test
    void publish_nullMessage_handlesGracefully() {
        // given
        when(payloadMapper.toPayload(null))
                .thenReturn(Mono.error(new IllegalArgumentException("메시지가 null입니다")));

        // when
        Mono<Void> result = notificationMessagePublish.publish(null);

        // then
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(payloadMapper).toPayload(null);
        verify(publishRouter, never()).publish(any(NotificationMessagePayload.class),
                any(NotificationType.class));
    }
}
