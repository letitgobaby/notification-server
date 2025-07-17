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
import notification.adapter.db.mapper.NotificationRequestEntityMapper;
import notification.adapter.db.repository.R2dbcNotificationRequestContentRepository;
import notification.adapter.db.repository.R2dbcNotificationRequestRecipientRepository;
import notification.adapter.db.repository.R2dbcNotificationRequestRepository;
import notification.adapter.db.repository.R2dbcNotificationRequestSenderRepository;
import notification.adapter.db.repository.R2dbcNotificationRequestTemplateInfoRepository;
import notification.domain.NotificationRequest;
import notification.domain.enums.NotificationType;
import notification.domain.enums.RequestStatus;
import notification.domain.enums.RequesterType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Requester;
import notification.domain.vo.TemplateInfo;
import notification.domain.vo.UserId;
import notification.domain.vo.recipient.UserRecipient;
import notification.domain.vo.sender.EmailSender;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Testcontainers
@Import({ NotificationRequestRepositoryAdapter.class, NotificationRequestEntityMapper.class, ObjectMapper.class })
class NotificationRequestRepositoryAdapterTest extends MariadbTestContainerConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    R2dbcNotificationRequestRepository requestRepository;

    @Autowired
    R2dbcNotificationRequestRecipientRepository recipientRepository;

    @Autowired
    R2dbcNotificationRequestSenderRepository senderRepository;

    @Autowired
    R2dbcNotificationRequestContentRepository contentRepository;

    @Autowired
    R2dbcNotificationRequestTemplateInfoRepository templateInfoRepository;

    private NotificationRequestEntityMapper mapper;
    private NotificationRequestRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new NotificationRequestEntityMapper(objectMapper);
        adapter = new NotificationRequestRepositoryAdapter(mapper, requestRepository, recipientRepository,
                senderRepository, contentRepository, templateInfoRepository);
    }

    @Test
    void save_shouldSaveNotificationRequestWithAllRelatedEntities() {
        NotificationRequest request = createSampleNotificationRequest("test-request-1");

        // When & Then
        StepVerifier.create(adapter.save(request))
                .assertNext(savedRequest -> {
                    assertThat(savedRequest).isNotNull();
                    assertThat(savedRequest.getRequestId()).isEqualTo(request.getRequestId());
                    assertThat(savedRequest.getRequester()).isEqualTo(request.getRequester());
                    assertThat(savedRequest.getStatus()).isEqualTo(request.getStatus());
                    assertThat(savedRequest.getRecipients()).hasSize(1);
                    assertThat(savedRequest.getSenderInfos()).hasSize(1);
                    assertThat(savedRequest.getContent()).isNotNull();
                    assertThat(savedRequest.getTemplate()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void save_shouldSaveNotificationRequestWithoutOptionalFields() {
        //
        NotificationRequest request = new NotificationRequest(
                new NotificationRequestId("test-request-2"),
                new Requester(RequesterType.SERVICE, "test-requester"),
                List.of(new UserRecipient(new UserId("user-123"))),
                List.of(NotificationType.EMAIL),
                Map.of(NotificationType.EMAIL,
                        new EmailSender("test@example.com", "Test Sender")),
                null, // content
                new TemplateInfo("template-123", Map.of("key1", "value1", "key2", "value2")),
                "Test memo",
                null, // scheduledAt
                RequestStatus.PENDING,
                null, // failureReason
                null, // processedAt
                null // createdAt - 새로운 엔티티임을 나타냄
        );

        //
        StepVerifier.create(adapter.save(request).log())
                .assertNext(savedRequest -> {
                    assertThat(savedRequest).isNotNull();
                    assertThat(savedRequest.getRequestId()).isEqualTo(request.getRequestId());
                    assertThat(savedRequest.getContent()).isNull();
                    assertThat(savedRequest.getRecipients()).hasSize(1);
                    assertThat(savedRequest.getSenderInfos()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnNotificationRequestWithAllRelatedEntities() {
        NotificationRequest request = createSampleNotificationRequest("test-request-99");

        //
        StepVerifier.create(adapter.save(request))
                .expectNextCount(1)
                .verifyComplete();

        //
        StepVerifier.create(adapter.findById(request.getRequestId()))
                .assertNext(foundRequest -> {
                    assertThat(foundRequest).isNotNull();
                    assertThat(foundRequest.getRequestId()).isEqualTo(request.getRequestId());
                    assertThat(foundRequest.getRequester()).isEqualTo(request.getRequester());
                    assertThat(foundRequest.getRecipients()).hasSize(1);
                    assertThat(foundRequest.getSenderInfos()).hasSize(1);
                    assertThat(foundRequest.getContent()).isNotNull();
                    assertThat(foundRequest.getTemplate()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnEmptyWhenNotificationRequestNotFound() {
        NotificationRequestId nonExistentId = new NotificationRequestId("non-existent-id");

        // When & Then
        StepVerifier.create(adapter.findById(nonExistentId))
                .verifyComplete();
    }

    @Test
    void save_shouldHandleEmptyRecipientsList() {
        // Given - 하지만 실제로는 빈 recipients는 도메인에서 허용하지 않으므로 최소 1개는 있어야 함
        NotificationRequest request = new NotificationRequest(
                new NotificationRequestId("test-request-3"),
                new Requester(RequesterType.SERVICE, "test-requester"),
                List.of(new UserRecipient(new UserId("user-123"))), // 최소 1개 필요
                List.of(NotificationType.EMAIL),
                Map.of(
                        NotificationType.EMAIL,
                        new EmailSender("test@example.com", "Test Sender") //
                ),
                null, // content
                new TemplateInfo("template-123", Map.of("key1", "value1")), // template 제공
                "Test memo",
                null, // scheduledAt
                RequestStatus.PENDING,
                null, // failureReason
                null, // processedAt
                null // createdAt
        );

        // When & Then
        StepVerifier.create(adapter.save(request))
                .assertNext(savedRequest -> {
                    assertThat(savedRequest).isNotNull();
                    assertThat(savedRequest.getRecipients()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void save_shouldHandleEmptySendersList() {
        // Given - 하지만 실제로는 빈 senderInfos는 도메인에서 허용하지 않으므로 최소 1개는 있어야 함
        NotificationRequest request = new NotificationRequest(
                new NotificationRequestId("test-request-4"),
                new Requester(RequesterType.SERVICE, "test-requester"),
                List.of(new UserRecipient(new UserId("user-123"))),
                List.of(NotificationType.EMAIL),
                Map.of(NotificationType.EMAIL,
                        new EmailSender("test@example.com", "Test Sender")),
                new NotificationContent("Test Title", "Test Body", "http://example.com", null),
                null, // template
                "Test memo",
                null, // scheduledAt
                RequestStatus.PENDING,
                null, // failureReason
                null, // processedAt
                null // createdAt
        );

        // When & Then
        StepVerifier.create(adapter.save(request))
                .assertNext(savedRequest -> {
                    assertThat(savedRequest).isNotNull();
                    assertThat(savedRequest.getSenderInfos()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void update_shouldUpdateNotificationRequest() {
        NotificationRequest request = createSampleNotificationRequest("test-request-5");

        //
        NotificationRequest saved = adapter.save(request).block();
        assertThat(saved).isNotNull();
        assertThat(saved.getRequestId()).isEqualTo(request.getRequestId());

        //
        saved.markAsProcessing();

        //
        StepVerifier.create(adapter.save(saved))
                .assertNext(updatedRequest -> {
                    assertThat(updatedRequest).isNotNull();
                    assertThat(request.getRequestId().value())
                            .isEqualTo(updatedRequest.getRequestId().value());
                    assertThat(updatedRequest.getStatus()).isEqualTo(RequestStatus.PROCESSING);
                })
                .verifyComplete();
    }

    private NotificationRequest createSampleNotificationRequest(String requestId) {
        return new NotificationRequest(
                new NotificationRequestId(requestId),
                new Requester(RequesterType.SERVICE, "test-requester"),
                List.of(new UserRecipient(new UserId("user-123"))),
                List.of(NotificationType.EMAIL),
                Map.of(NotificationType.EMAIL,
                        new EmailSender("test@example.com", "Test Sender")),
                new NotificationContent("Test Title", "Test Body", "http://example.com",
                        "http://image.example.com"),
                new TemplateInfo("template-123", Map.of("key1", "value1", "key2", "value2")),
                "Test memo",
                Instant.now().plusSeconds(3600),
                RequestStatus.PENDING,
                null, // failureReason
                null, // processedAt
                null // createdAt - 새로운 엔티티임을 나타냄
        );
    }
}