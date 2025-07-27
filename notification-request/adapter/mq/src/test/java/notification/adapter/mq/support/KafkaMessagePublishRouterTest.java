package notification.adapter.mq.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import notification.adapter.mq.payload.EmailMessagePayload;
import notification.adapter.mq.payload.NotificationMessagePayload;
import notification.adapter.mq.payload.PushMessagePayload;
import notification.adapter.mq.payload.SmsMessagePayload;
import notification.domain.enums.NotificationType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaMessagePublishRouter 테스트")
class KafkaMessagePublishRouterTest {

    @Mock
    private KafkaEmailMessagePublish emailPublisher;

    @Mock
    private KafkaSmsMessagePublish smsPublisher;

    @Mock
    private KafkaPushMessagePublish pushPublisher;

    private KafkaMessagePublishRouter router;
    private EmailMessagePayload emailPayload;
    private SmsMessagePayload smsPayload;
    private PushMessagePayload pushPayload;

    @BeforeEach
    void setUp() {
        // Mock publishers 설정 (lenient 모드로 설정)
        lenient().when(emailPublisher.getType()).thenReturn(NotificationType.EMAIL);
        lenient().when(smsPublisher.getType()).thenReturn(NotificationType.SMS);
        lenient().when(pushPublisher.getType()).thenReturn(NotificationType.PUSH);

        // Publisher 리스트로 라우터 생성
        List<KafkaMessagePublishSupport<? extends NotificationMessagePayload>> publishers = List.of(
                emailPublisher, smsPublisher, pushPublisher);
        router = new KafkaMessagePublishRouter(publishers);

        // 테스트용 페이로드 설정
        Instant now = Instant.now();

        emailPayload = EmailMessagePayload.builder()
                .messageId("email-msg-123")
                .requestId("req-123")
                .createdAt(now)
                .subject("Test Subject")
                .body("Test Body")
                .senderEmail("sender@example.com")
                .senderName("Test Sender")
                .recipientEmail("recipient@example.com")
                .build();

        smsPayload = SmsMessagePayload.builder()
                .messageId("sms-msg-123")
                .requestId("req-123")
                .createdAt(now)
                .senderPhone("01000000000")
                .recipientPhone("01012345678")
                .messageText("Test SMS Message")
                .build();

        pushPayload = PushMessagePayload.builder()
                .messageId("push-msg-123")
                .requestId("req-123")
                .createdAt(now)
                .deviceToken("device-token-123")
                .title("Push Title")
                .body("Push Body")
                .imageUrl("https://example.com/image.jpg")
                .redirectUrl("https://example.com/redirect")
                .senderName("Push Sender")
                .build();
    }

    @DisplayName("EMAIL 타입 메시지 라우팅 성공")
    @Test
    void publish_emailType_routesToEmailPublisher() {
        // given
        when(emailPublisher.publish(emailPayload)).thenReturn(Mono.empty());

        // when
        Mono<Void> result = router.publish(emailPayload, NotificationType.EMAIL);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(emailPublisher).publish(emailPayload);
        verify(smsPublisher, never()).publish(any());
        verify(pushPublisher, never()).publish(any());
    }

    @DisplayName("SMS 타입 메시지 라우팅 성공")
    @Test
    void publish_smsType_routesToSmsPublisher() {
        // given
        when(smsPublisher.publish(smsPayload)).thenReturn(Mono.empty());

        // when
        Mono<Void> result = router.publish(smsPayload, NotificationType.SMS);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(smsPublisher).publish(smsPayload);
        verify(emailPublisher, never()).publish(any());
        verify(pushPublisher, never()).publish(any());
    }

    @DisplayName("PUSH 타입 메시지 라우팅 성공")
    @Test
    void publish_pushType_routesToPushPublisher() {
        // given
        when(pushPublisher.publish(pushPayload)).thenReturn(Mono.empty());

        // when
        Mono<Void> result = router.publish(pushPayload, NotificationType.PUSH);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(pushPublisher).publish(pushPayload);
        verify(emailPublisher, never()).publish(any());
        verify(smsPublisher, never()).publish(any());
    }

    @DisplayName("지원되지 않는 NotificationType인 경우 예외 발생")
    @Test
    void publish_unsupportedType_throwsException() {
        // given - 빈 publisher 리스트로 라우터 생성
        KafkaMessagePublishRouter emptyRouter = new KafkaMessagePublishRouter(List.of());

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> emptyRouter.publish(emailPayload, NotificationType.EMAIL));

        assertEquals("No publisher for type: EMAIL", exception.getMessage());
    }

    @DisplayName("Publisher에서 예외 발생 시 예외 전파")
    @Test
    void publish_publisherThrowsException_propagatesException() {
        // given
        RuntimeException expectedException = new RuntimeException("Publishing failed");
        when(emailPublisher.publish(emailPayload)).thenReturn(Mono.error(expectedException));

        // when
        Mono<Void> result = router.publish(emailPayload, NotificationType.EMAIL);

        // then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(emailPublisher).publish(emailPayload);
    }

    @DisplayName("올바른 Publisher 찾기 로직 검증")
    @Test
    void findPublisher_correctMapping() {
        // EMAIL 타입으로 발행 시 EMAIL publisher 사용
        when(emailPublisher.publish(emailPayload)).thenReturn(Mono.empty());

        StepVerifier.create(router.publish(emailPayload, NotificationType.EMAIL))
                .verifyComplete();

        verify(emailPublisher).publish(emailPayload);

        // SMS 타입으로 발행 시 SMS publisher 사용
        when(smsPublisher.publish(smsPayload)).thenReturn(Mono.empty());

        StepVerifier.create(router.publish(smsPayload, NotificationType.SMS))
                .verifyComplete();

        verify(smsPublisher).publish(smsPayload);

        // PUSH 타입으로 발행 시 PUSH publisher 사용
        when(pushPublisher.publish(pushPayload)).thenReturn(Mono.empty());

        StepVerifier.create(router.publish(pushPayload, NotificationType.PUSH))
                .verifyComplete();

        verify(pushPublisher).publish(pushPayload);
    }

    @DisplayName("동일한 타입의 publisher가 여러 개인 경우 첫 번째 선택")
    @Test
    void findPublisher_multiplePublishersOfSameType_selectsFirst() {
        // given - 같은 타입의 publisher 두 개 생성
        KafkaEmailMessagePublish secondEmailPublisher = mock(KafkaEmailMessagePublish.class);
        lenient().when(secondEmailPublisher.getType()).thenReturn(NotificationType.EMAIL);

        List<KafkaMessagePublishSupport<? extends NotificationMessagePayload>> publishersWithDuplicate = List.of(
                emailPublisher, secondEmailPublisher, smsPublisher, pushPublisher);
        KafkaMessagePublishRouter routerWithDuplicate = new KafkaMessagePublishRouter(publishersWithDuplicate);

        when(emailPublisher.publish(emailPayload)).thenReturn(Mono.empty());

        // when
        StepVerifier.create(routerWithDuplicate.publish(emailPayload, NotificationType.EMAIL))
                .verifyComplete();

        // then - 첫 번째 publisher만 사용됨
        verify(emailPublisher).publish(emailPayload);
        verify(secondEmailPublisher, never()).publish(any());
    }

    @DisplayName("빈 publisher 리스트로 모든 타입에 대해 예외 발생")
    @Test
    void publish_emptyPublisherList_throwsExceptionForAllTypes() {
        // given
        KafkaMessagePublishRouter emptyRouter = new KafkaMessagePublishRouter(List.of());

        // EMAIL 타입 테스트
        IllegalArgumentException emailException = assertThrows(
                IllegalArgumentException.class,
                () -> emptyRouter.publish(emailPayload, NotificationType.EMAIL));
        assertEquals("No publisher for type: EMAIL", emailException.getMessage());

        // SMS 타입 테스트
        IllegalArgumentException smsException = assertThrows(
                IllegalArgumentException.class,
                () -> emptyRouter.publish(smsPayload, NotificationType.SMS));
        assertEquals("No publisher for type: SMS", smsException.getMessage());

        // PUSH 타입 테스트
        IllegalArgumentException pushException = assertThrows(
                IllegalArgumentException.class,
                () -> emptyRouter.publish(pushPayload, NotificationType.PUSH));
        assertEquals("No publisher for type: PUSH", pushException.getMessage());
    }

    @DisplayName("연속된 여러 메시지 발행")
    @Test
    void publish_multipleConsecutiveMessages_success() {
        // given
        when(emailPublisher.publish(emailPayload)).thenReturn(Mono.empty());
        when(smsPublisher.publish(smsPayload)).thenReturn(Mono.empty());
        when(pushPublisher.publish(pushPayload)).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(router.publish(emailPayload, NotificationType.EMAIL))
                .verifyComplete();

        StepVerifier.create(router.publish(smsPayload, NotificationType.SMS))
                .verifyComplete();

        StepVerifier.create(router.publish(pushPayload, NotificationType.PUSH))
                .verifyComplete();

        // 모든 publisher가 한 번씩 호출되었는지 확인
        verify(emailPublisher).publish(emailPayload);
        verify(smsPublisher).publish(smsPayload);
        verify(pushPublisher).publish(pushPayload);
    }
}
