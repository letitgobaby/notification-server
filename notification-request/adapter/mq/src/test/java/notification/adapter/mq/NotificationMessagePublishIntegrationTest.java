package notification.adapter.mq;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import notification.adapter.mq.payload.EmailMessagePayload;
import notification.adapter.mq.payload.PushMessagePayload;
import notification.adapter.mq.payload.SmsMessagePayload;
import notification.domain.NotificationMessage;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SmsSender;
import reactor.test.StepVerifier;

@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@SpringBootTest(classes = TestMessageQueueApplication.class, properties = {
        "app.kafka.topic.notification=test-notification-topic"
})
@DisplayName("NotificationMessagePublish 실제 Kafka 통합 테스트")
class NotificationMessagePublishIntegrationTest extends KafkaTestContainerConfig {

    @Autowired
    private NotificationMessagePublish notificationMessagePublish;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private final String emailTopicName = "test-notification-topic.email";
    private final String smsTopicName = "test-notification-topic.sms";
    private final String pushTopicName = "test-notification-topic.push";

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "testGroup-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        consumer = new KafkaConsumer<>(consumerProps);
        // 각 테스트에서 특정 토픽을 구독하도록 변경
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @DisplayName("EMAIL 메시지를 실제 Kafka에 발행")
    @Test
    void publish_emailMessage_shouldSendToRealKafka() throws Exception {
        // given
        NotificationRequestId requestId = NotificationRequestId.create();
        Recipient recipient = new Recipient("user123", "recipient@example.com", "01012345678", "device-token",
                "ko");
        NotificationContent content = new NotificationContent("통합 테스트 제목", "통합 테스트 내용", "https://example.com",
                "https://image.com");
        EmailSender senderInfo = new EmailSender("sender-id", "sender@example.com", "발송자");

        NotificationMessage message = NotificationMessage.create(
                requestId, NotificationType.EMAIL, recipient, content, senderInfo, Instant.now());

        // when
        StepVerifier.create(notificationMessagePublish.publish(message))
                .verifyComplete();

        // then
        consumer.subscribe(Collections.singleton(emailTopicName));

        // Get all records instead of expecting a single one
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        Iterable<ConsumerRecord<String, String>> topicRecords = records.records(emailTopicName);

        // Find the record for our specific message
        ConsumerRecord<String, String> singleRecord = null;
        for (ConsumerRecord<String, String> record : topicRecords) {
            if (message.getMessageId().value().equals(record.key())) {
                singleRecord = record;
                break;
            }
        }

        assertNotNull(singleRecord, "Expected to find a record with key: " + message.getMessageId().value());
        assertEquals(emailTopicName, singleRecord.topic());
        assertEquals(message.getMessageId().value(), singleRecord.key());

        // Deserialize and validate payload
        EmailMessagePayload received = objectMapper.readValue(singleRecord.value(), EmailMessagePayload.class);

        assertNotNull(received);
        assertEquals(message.getMessageId().value(), received.getMessageId());
        assertEquals(message.getRequestId().value(), received.getRequestId());
        assertEquals("통합 테스트 제목", received.getSubject());
        assertEquals("통합 테스트 내용", received.getBody());
        assertEquals("sender@example.com", received.getSenderEmail());
        assertEquals("발송자", received.getSenderName());
        assertEquals("recipient@example.com", received.getRecipientEmail());
    }

    @DisplayName("SMS 메시지를 실제 Kafka에 발행")
    @Test
    void publish_smsMessage_shouldSendToRealKafka() throws Exception {
        // given
        NotificationRequestId requestId = NotificationRequestId.create();
        Recipient recipient = new Recipient("user123", "recipient@example.com", "01012345678", "device-token",
                "ko");
        NotificationContent content = new NotificationContent("SMS 제목", "SMS 통합 테스트 메시지", "https://example.com",
                "https://image.com");
        SmsSender senderInfo = new SmsSender("sender-id", "01000000000", "발송자");

        NotificationMessage message = NotificationMessage.create(
                requestId, NotificationType.SMS, recipient, content, senderInfo, Instant.now());

        // when
        StepVerifier.create(notificationMessagePublish.publish(message))
                .verifyComplete();

        // then
        consumer.subscribe(Collections.singleton(smsTopicName));
        ConsumerRecord<String, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, smsTopicName,
                Duration.ofSeconds(10));

        assertNotNull(singleRecord);
        assertEquals(smsTopicName, singleRecord.topic());
        assertEquals(message.getMessageId().value(), singleRecord.key());

        // Deserialize and validate payload
        SmsMessagePayload received = objectMapper.readValue(singleRecord.value(), SmsMessagePayload.class);

        assertNotNull(received);
        assertEquals(message.getMessageId().value(), received.getMessageId());
        assertEquals(message.getRequestId().value(), received.getRequestId());
        assertEquals("01000000000", received.getSenderPhone());
        assertEquals("01012345678", received.getRecipientPhone());
        assertEquals("SMS 통합 테스트 메시지", received.getMessageText());
    }

    @DisplayName("PUSH 메시지를 실제 Kafka에 발행")
    @Test
    void publish_pushMessage_shouldSendToRealKafka() throws Exception {
        // given
        NotificationRequestId requestId = NotificationRequestId.create();
        Recipient recipient = new Recipient("user123", "recipient@example.com", "01012345678",
                "device-token-123",
                "ko");
        NotificationContent content = new NotificationContent("푸시 제목", "푸시 내용", "https://example.com/redirect",
                "https://example.com/image.jpg");
        PushSender senderInfo = new PushSender("sender-id", "푸시 발송자");

        NotificationMessage message = NotificationMessage.create(
                requestId, NotificationType.PUSH, recipient, content, senderInfo, Instant.now());

        // when
        StepVerifier.create(notificationMessagePublish.publish(message))
                .verifyComplete();

        // then
        consumer.subscribe(Collections.singleton(pushTopicName));
        ConsumerRecord<String, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, pushTopicName,
                Duration.ofSeconds(10));

        assertNotNull(singleRecord);
        assertEquals(pushTopicName, singleRecord.topic());
        assertEquals(message.getMessageId().value(), singleRecord.key());

        // Deserialize and validate payload
        PushMessagePayload received = objectMapper.readValue(singleRecord.value(), PushMessagePayload.class);

        assertNotNull(received);
        assertEquals(message.getMessageId().value(), received.getMessageId());
        assertEquals(message.getRequestId().value(), received.getRequestId());
        assertEquals("device-token-123", received.getDeviceToken());
        assertEquals("푸시 제목", received.getTitle());
        assertEquals("푸시 내용", received.getBody());
        assertEquals("https://example.com/image.jpg", received.getImageUrl());
        assertEquals("https://example.com/redirect", received.getRedirectUrl());
        assertEquals("푸시 발송자", received.getSenderName());
    }

    @DisplayName("여러 타입의 메시지를 연속으로 실제 Kafka에 발행")
    @Test
    void publish_multipleMessageTypes_shouldSendAllToRealKafka() throws Exception {
        // given
        NotificationRequestId emailRequestId = NotificationRequestId.create();
        Recipient emailRecipient = new Recipient("user123", "recipient@example.com", "01012345678",
                "device-token",
                "ko");
        NotificationContent emailContent = new NotificationContent("멀티 이메일", "멀티 이메일 내용", "https://example.com",
                "https://image.com");
        EmailSender emailSenderInfo = new EmailSender("sender-id", "sender@example.com", "발송자");

        NotificationMessage emailMessage = NotificationMessage.create(
                emailRequestId, NotificationType.EMAIL, emailRecipient, emailContent, emailSenderInfo,
                Instant.now());

        NotificationRequestId smsRequestId = NotificationRequestId.create();
        Recipient smsRecipient = new Recipient("user456", "recipient2@example.com", "01087654321",
                "device-token2",
                "ko");
        NotificationContent smsContent = new NotificationContent("멀티 SMS 제목", "멀티 SMS", "https://example.com",
                "https://image.com");
        SmsSender smsSenderInfo = new SmsSender("sender-id", "01000000000", "SMS 발송자");

        NotificationMessage smsMessage = NotificationMessage.create(
                smsRequestId, NotificationType.SMS, smsRecipient, smsContent, smsSenderInfo,
                Instant.now());

        // when
        StepVerifier.create(notificationMessagePublish.publish(emailMessage))
                .verifyComplete();

        StepVerifier.create(notificationMessagePublish.publish(smsMessage))
                .verifyComplete();

        // then
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "multiTypeTestGroup-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (Consumer<String, String> multiConsumer = new KafkaConsumer<>(consumerProps)) {
            // 모든 토픽을 구독
            multiConsumer.subscribe(List.of(emailTopicName, smsTopicName));

            // Get all records from both topics
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(multiConsumer,
                    Duration.ofSeconds(10));

            // Find records for our specific messages
            ConsumerRecord<String, String> emailRecord = null;
            ConsumerRecord<String, String> smsRecord = null;

            for (ConsumerRecord<String, String> record : records.records(emailTopicName)) {
                if (emailMessage.getMessageId().value().equals(record.key())) {
                    emailRecord = record;
                    break;
                }
            }

            for (ConsumerRecord<String, String> record : records.records(smsTopicName)) {
                if (smsMessage.getMessageId().value().equals(record.key())) {
                    smsRecord = record;
                    break;
                }
            }

            assertNotNull(emailRecord, "Expected to find email record");
            assertNotNull(smsRecord, "Expected to find SMS record");

            // 메시지 ID로 구분
            String emailMessageId = emailRecord.key();
            String smsMessageId = smsRecord.key();

            assertEquals(emailMessage.getMessageId().value(), emailMessageId);
            assertEquals(smsMessage.getMessageId().value(), smsMessageId);

            // 각 레코드의 페이로드 타입 확인
            EmailMessagePayload emailPayload = objectMapper.readValue(emailRecord.value(),
                    EmailMessagePayload.class);
            assertEquals("멀티 이메일", emailPayload.getSubject());

            SmsMessagePayload smsPayload = objectMapper.readValue(smsRecord.value(),
                    SmsMessagePayload.class);
            assertEquals("멀티 SMS", smsPayload.getMessageText());
        }
    }
}
