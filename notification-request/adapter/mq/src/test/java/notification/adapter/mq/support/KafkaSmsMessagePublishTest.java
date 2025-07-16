package notification.adapter.mq.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import notification.adapter.mq.payload.SmsMessagePayload;
import notification.definition.exceptions.ObjectConversionException;
import notification.domain.enums.NotificationType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaSmsMessagePublish 테스트")
class KafkaSmsMessagePublishTest {

    @Mock
    private KafkaSender<String, String> kafkaSender;

    @Mock
    private ObjectMapper objectMapper;

    private KafkaSmsMessagePublish kafkaSmsMessagePublish;

    @BeforeEach
    void setUp() {
        kafkaSmsMessagePublish = new KafkaSmsMessagePublish(kafkaSender, objectMapper);
        // Set the notificationTopic field via reflection since it's injected by @Value
        setNotificationTopic("test-notification-topic");
    }

    private void setNotificationTopic(String topic) {
        try {
            var field = KafkaSmsMessagePublish.class.getDeclaredField("notificationTopic");
            field.setAccessible(true);
            field.set(kafkaSmsMessagePublish, topic);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DisplayName("getType()은 SMS를 반환한다")
    @Test
    void getType_returnsSms() {
        assertEquals(NotificationType.SMS, kafkaSmsMessagePublish.getType());
    }

    @DisplayName("SMS 메시지 발행 성공")
    @Test
    void publish_successful() throws Exception {
        // given
        SmsMessagePayload payload = SmsMessagePayload.builder()
                .messageId("sms-msg-1")
                .requestId("sms-req-1")
                .senderPhone("01000000000")
                .recipientPhone("01012345678")
                .messageText("테스트 SMS 메시지")
                .createdAt(Instant.now())
                .build();

        String serializedPayload = "{\"messageId\":\"sms-msg-1\",\"senderPhone\":\"01000000000\"}";
        when(objectMapper.writeValueAsString(payload)).thenReturn(serializedPayload);

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(senderResult.exception()).thenReturn(null);

        when(kafkaSender.send(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Flux<SenderRecord<String, String, String>> flux = invocation.getArgument(0);
            return flux.map(sr -> senderResult);
        });

        // when
        Mono<Void> result = kafkaSmsMessagePublish.publish(payload);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper).writeValueAsString(payload);
        verify(kafkaSender).send(any());
    }

    @DisplayName("직렬화 실패 시 ObjectConversionException 발생")
    @Test
    void publish_serializationFails_shouldReturnObjectConversionException() throws Exception {
        // given
        SmsMessagePayload payload = SmsMessagePayload.builder()
                .messageId("sms-msg-2")
                .requestId("sms-req-2")
                .senderPhone("01000000000")
                .recipientPhone("01012345678")
                .messageText("테스트 SMS")
                .createdAt(Instant.now())
                .build();

        when(objectMapper.writeValueAsString(payload))
                .thenThrow(new JsonProcessingException("직렬화 실패") {
                });

        // when
        Mono<Void> result = kafkaSmsMessagePublish.publish(payload);

        // then
        StepVerifier.create(result)
                .expectError(ObjectConversionException.class)
                .verify();

        verify(objectMapper).writeValueAsString(payload);
        verify(kafkaSender, never()).send(any());
    }

    @DisplayName("Kafka 전송 실패 시 예외 전파")
    @Test
    void publish_kafkaSendFails_shouldPropagateException() throws Exception {
        // given
        SmsMessagePayload payload = SmsMessagePayload.builder()
                .messageId("sms-msg-3")
                .requestId("sms-req-3")
                .senderPhone("01000000000")
                .recipientPhone("01012345678")
                .messageText("테스트 SMS")
                .createdAt(Instant.now())
                .build();

        String serializedPayload = "{\"messageId\":\"sms-msg-3\"}";
        when(objectMapper.writeValueAsString(payload)).thenReturn(serializedPayload);

        Exception kafkaException = new RuntimeException("Kafka 전송 실패");

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(senderResult.exception()).thenReturn(kafkaException);

        when(kafkaSender.send(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Flux<SenderRecord<String, String, String>> flux = invocation.getArgument(0);
            return flux.map(sr -> senderResult);
        });

        // when
        Mono<Void> result = kafkaSmsMessagePublish.publish(payload);

        // then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(objectMapper).writeValueAsString(payload);
        verify(kafkaSender).send(any());
    }

    @DisplayName("빈 메시지 텍스트로 SMS 발행")
    @Test
    void publish_withEmptyMessageText_shouldSucceed() throws Exception {
        // given
        SmsMessagePayload payload = SmsMessagePayload.builder()
                .messageId("sms-msg-4")
                .requestId("sms-req-4")
                .senderPhone("01000000000")
                .recipientPhone("01012345678")
                .messageText("")
                .createdAt(Instant.now())
                .build();

        String serializedPayload = "{\"messageText\":\"\"}";
        when(objectMapper.writeValueAsString(payload)).thenReturn(serializedPayload);

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(senderResult.exception()).thenReturn(null);

        when(kafkaSender.send(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Flux<SenderRecord<String, String, String>> flux = invocation.getArgument(0);
            return flux.map(sr -> senderResult);
        });

        // when
        Mono<Void> result = kafkaSmsMessagePublish.publish(payload);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper).writeValueAsString(payload);
        verify(kafkaSender).send(any());
    }

    @DisplayName("긴 메시지로 SMS 발행")
    @Test
    void publish_withLongMessage_shouldSucceed() throws Exception {
        // given
        String longMessage = "긴 메시지입니다. ".repeat(50);
        SmsMessagePayload payload = SmsMessagePayload.builder()
                .messageId("sms-msg-5")
                .requestId("sms-req-5")
                .senderPhone("01000000000")
                .recipientPhone("01012345678")
                .messageText(longMessage)
                .createdAt(Instant.now())
                .build();

        String serializedPayload = "{\"messageText\":\"" + longMessage + "\"}";
        when(objectMapper.writeValueAsString(payload)).thenReturn(serializedPayload);

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(senderResult.exception()).thenReturn(null);

        when(kafkaSender.send(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Flux<SenderRecord<String, String, String>> flux = invocation.getArgument(0);
            return flux.map(sr -> senderResult);
        });

        // when
        Mono<Void> result = kafkaSmsMessagePublish.publish(payload);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper).writeValueAsString(payload);
        verify(kafkaSender).send(any());
    }

    @DisplayName("국제 전화번호로 SMS 발행")
    @Test
    void publish_withInternationalPhoneNumbers_shouldSucceed() throws Exception {
        // given
        SmsMessagePayload payload = SmsMessagePayload.builder()
                .messageId("sms-msg-6")
                .requestId("sms-req-6")
                .senderPhone("+82-10-0000-0000")
                .recipientPhone("+1-555-123-4567")
                .messageText("국제 SMS 테스트")
                .createdAt(Instant.now())
                .build();

        String serializedPayload = "{\"senderPhone\":\"+82-10-0000-0000\",\"recipientPhone\":\"+1-555-123-4567\"}";
        when(objectMapper.writeValueAsString(payload)).thenReturn(serializedPayload);

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(senderResult.exception()).thenReturn(null);

        when(kafkaSender.send(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Flux<SenderRecord<String, String, String>> flux = invocation.getArgument(0);
            return flux.map(sr -> senderResult);
        });

        // when
        Mono<Void> result = kafkaSmsMessagePublish.publish(payload);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper).writeValueAsString(payload);
        verify(kafkaSender).send(any());
    }
}