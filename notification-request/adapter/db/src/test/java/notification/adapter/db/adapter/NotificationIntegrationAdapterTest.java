package notification.adapter.db.adapter;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import notification.adapter.db.MariadbTestContainerConfig;
import notification.adapter.db.mapper.NotificationMessageEntityMapper;
import notification.adapter.db.mapper.NotificationRequestEntityMapper;
import notification.domain.NotificationMessage;
import notification.domain.NotificationRequest;
import notification.domain.enums.DeliveryStatus;
import notification.domain.enums.NotificationType;
import notification.domain.enums.RequestStatus;
import notification.domain.enums.RequesterType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Requester;
import notification.domain.vo.TemplateInfo;
import notification.domain.vo.UserId;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.recipient.RecipientReference;
import notification.domain.vo.recipient.UserRecipient;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.SenderInfo;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Testcontainers
@Import({
        NotificationMessageRepositoryAdapter.class,
        NotificationMessageEntityMapper.class,
        NotificationRequestRepositoryAdapter.class,
        NotificationRequestEntityMapper.class,
        ObjectMapper.class
})
public class NotificationIntegrationAdapterTest extends MariadbTestContainerConfig {

    @Autowired
    private NotificationRequestRepositoryAdapter requestAdapter;

    @Autowired
    private NotificationMessageRepositoryAdapter messageAdapter;

    private NotificationRequest savedRequest;

    @BeforeEach
    void setUp() {
        // 테스트마다 새로운 NotificationRequest를 생성하고 저장
        NotificationRequest request = createTestNotificationRequest();

        StepVerifier.create(requestAdapter.save(request))
                .assertNext(saved -> {
                    assertThat(saved).isNotNull();
                    this.savedRequest = saved;
                })
                .verifyComplete();
    }

    @Test
    void shouldSaveAndRetrieveNotificationMessageWithExistingRequest() {
        NotificationMessage message = createNotificationMessage(savedRequest.getRequestId());

        //
        StepVerifier.create(messageAdapter.save(message))
                .assertNext(savedMessage -> {
                    assertThat(savedMessage).isNotNull();
                    assertThat(savedMessage.getMessageId()).isEqualTo(message.getMessageId());
                    assertThat(savedMessage.getRequestId()).isEqualTo(savedRequest.getRequestId());
                    assertThat(savedMessage.getNotificationType())
                            .isEqualTo(NotificationType.EMAIL);
                    assertThat(savedMessage.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
                    assertThat(savedMessage.getRecipient().email()).isEqualTo("test@example.com");
                })
                .verifyComplete();

        //
        StepVerifier.create(messageAdapter.findById(message.getMessageId()))
                .assertNext(foundMessage -> {
                    assertThat(foundMessage.getRequestId()).isEqualTo(savedRequest.getRequestId());
                    assertThat(foundMessage.getNotificationType())
                            .isEqualTo(NotificationType.EMAIL);
                    assertThat(foundMessage.getRecipient().email()).isEqualTo("test@example.com");
                })
                .verifyComplete();
    }

    @Test
    void shouldUpdateNotificationMessageStatus() {
        NotificationMessage originalMessage = createNotificationMessage(savedRequest.getRequestId());

        NotificationMessage savedMessage = messageAdapter.save(originalMessage).block();

        //
        savedMessage.markAsDispatched();

        StepVerifier.create(messageAdapter.update(savedMessage))
                .assertNext(updated -> {
                    assertThat(updated.getDeliveryStatus()).isEqualTo(DeliveryStatus.DISPATCHED);
                    assertThat(updated.getDispatchedAt()).isNotNull();
                })
                .verifyComplete();

        //
        StepVerifier.create(messageAdapter.findById(originalMessage.getMessageId()))
                .assertNext(foundMessage -> {
                    assertThat(foundMessage.getDeliveryStatus())
                            .isEqualTo(DeliveryStatus.DISPATCHED);
                    assertThat(foundMessage.getDispatchedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void shouldDeleteNotificationMessage() {
        NotificationMessage message = createNotificationMessage(savedRequest.getRequestId());

        StepVerifier.create(messageAdapter.save(message))
                .assertNext(savedMessage -> assertThat(savedMessage).isNotNull())
                .verifyComplete();

        //
        StepVerifier.create(messageAdapter.deleteById(message.getMessageId()))
                .verifyComplete();

        //
        StepVerifier.create(messageAdapter.findById(message.getMessageId()))
                .verifyComplete();
    }

    @Test
    void shouldHandleMultipleMessagesForSameRequest() {
        NotificationMessage emailMessage = createNotificationMessage(savedRequest.getRequestId());
        NotificationMessage smsMessage = createSmsNotificationMessage(savedRequest.getRequestId());

        //
        StepVerifier.create(messageAdapter.save(emailMessage))
                .assertNext(saved -> assertThat(saved.getNotificationType())
                        .isEqualTo(NotificationType.EMAIL))
                .verifyComplete();

        StepVerifier.create(messageAdapter.save(smsMessage))
                .assertNext(saved -> assertThat(saved.getNotificationType())
                        .isEqualTo(NotificationType.SMS))
                .verifyComplete();

        //
        StepVerifier.create(messageAdapter.findById(emailMessage.getMessageId()))
                .assertNext(found -> {
                    assertThat(found.getRequestId()).isEqualTo(savedRequest.getRequestId());
                    assertThat(found.getNotificationType()).isEqualTo(NotificationType.EMAIL);
                })
                .verifyComplete();

        StepVerifier.create(messageAdapter.findById(smsMessage.getMessageId()))
                .assertNext(found -> {
                    assertThat(found.getRequestId()).isEqualTo(savedRequest.getRequestId());
                    assertThat(found.getNotificationType()).isEqualTo(NotificationType.SMS);
                })
                .verifyComplete();
    }

    private NotificationRequest createTestNotificationRequest() {
        NotificationRequestId requestId = new NotificationRequestId("test-req-" + System.currentTimeMillis());
        Requester requester = new Requester(RequesterType.USER, "user-123");

        UserRecipient recipient = new UserRecipient(new UserId("user-123"));
        List<RecipientReference> recipients = List.of(recipient);

        List<NotificationType> notificationTypes = List.of(NotificationType.EMAIL, NotificationType.SMS);

        Map<NotificationType, SenderInfo> senderInfos = Map.of(
                NotificationType.EMAIL,
                new EmailSender("sender@example.com", "Test Sender"));

        NotificationContent content = new NotificationContent(
                "Test Title",
                "Test Body",
                "http://example.com",
                "http://example.com/image.jpg");

        TemplateInfo template = new TemplateInfo("template-123", Map.of("key", "value"));

        return new NotificationRequest(
                requestId,
                requester,
                recipients,
                notificationTypes,
                senderInfos,
                content,
                template,
                "Test memo",
                Instant.now().plusSeconds(3600), // 1 hour from now
                RequestStatus.PENDING,
                null,
                null,
                null);
    }

    private NotificationMessage createNotificationMessage(NotificationRequestId requestId) {
        return new NotificationMessage(
                new NotificationMessageId("msg-" + System.currentTimeMillis()),
                requestId,
                NotificationType.EMAIL,
                new Recipient("user-123", "test@example.com", "010-1234-5678", "device-token", "ko"),
                new NotificationContent("Email Title", "Email Body",
                        "http://example.com",
                        "http://example.com/image.jpg"),
                new EmailSender("sender@example.com", "Email Sender"),
                DeliveryStatus.PENDING,
                Instant.now(),
                null,
                null,
                null // createdAt will be set by the adapter
        );
    }

    private NotificationMessage createSmsNotificationMessage(NotificationRequestId requestId) {
        return new NotificationMessage(
                new NotificationMessageId("sms-msg-" + System.currentTimeMillis()),
                requestId,
                NotificationType.SMS,
                new Recipient("user-123", null, "010-1234-5678", null, "ko"),
                new NotificationContent("SMS Title", "SMS Body", null, null),
                new EmailSender("010-9876-5432", "SMS Sender"),
                DeliveryStatus.PENDING,
                Instant.now(),
                null,
                null,
                null // createdAt will be set by the adapter
        );
    }
}
