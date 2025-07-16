package notification.adapter.db.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import notification.adapter.db.MariadbTestContainerConfig;
import notification.adapter.db.NotificationMessageEntity;
import notification.adapter.db.mapper.NotificationMessageEntityMapper;
import notification.adapter.db.repository.R2dbcNotificationMessageRepository;
import notification.definition.exceptions.DataNotFoundException;
import notification.domain.NotificationMessage;
import notification.domain.enums.DeliveryStatus;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.sender.EmailSender;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Testcontainers
@Import({ NotificationMessageRepositoryAdapter.class, NotificationMessageEntityMapper.class })
class NotificationMessageRepositoryAdapterTest extends MariadbTestContainerConfig {

    @Mock
    private NotificationMessageEntityMapper mapper;

    @Mock
    private R2dbcNotificationMessageRepository messageRepository;

    private NotificationMessageRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new NotificationMessageRepositoryAdapter(mapper, messageRepository);
    }

    @Test
    void save_shouldSaveNotificationMessage() {
        // Given
        NotificationMessage domain = createSampleNotificationMessage();
        NotificationMessageEntity entity = createSampleNotificationMessageEntity();

        given(mapper.toEntity(domain)).willReturn(entity);
        given(messageRepository.save(entity)).willReturn(Mono.just(entity));
        given(mapper.toDomain(entity)).willReturn(domain);

        // When & Then
        StepVerifier.create(adapter.save(domain))
                .assertNext(savedMessage -> {
                    assertThat(savedMessage).isNotNull();
                    assertThat(savedMessage.getMessageId()).isEqualTo(domain.getMessageId());
                    assertThat(savedMessage.getNotificationType()).isEqualTo(domain.getNotificationType());
                    assertThat(savedMessage.getDeliveryStatus()).isEqualTo(domain.getDeliveryStatus());
                })
                .verifyComplete();

        then(mapper).should().toEntity(domain);
        then(messageRepository).should().save(entity);
        then(mapper).should().toDomain(entity);
    }

    @Test
    void save_shouldHandleMapperException() {
        // Given
        NotificationMessage domain = createSampleNotificationMessage();
        RuntimeException exception = new RuntimeException("Mapping error");

        given(mapper.toEntity(domain)).willThrow(exception);

        // When & Then
        StepVerifier.create(adapter.save(domain))
                .expectError(RuntimeException.class)
                .verify();

        then(mapper).should().toEntity(domain);
        then(messageRepository).should(never()).save(any());
    }

    @Test
    void save_shouldHandleRepositoryException() {
        // Given
        NotificationMessage domain = createSampleNotificationMessage();
        NotificationMessageEntity entity = createSampleNotificationMessageEntity();
        RuntimeException exception = new RuntimeException("Database error");

        given(mapper.toEntity(domain)).willReturn(entity);
        given(messageRepository.save(entity)).willReturn(Mono.error(exception));

        // When & Then
        StepVerifier.create(adapter.save(domain))
                .expectError(RuntimeException.class)
                .verify();

        then(mapper).should().toEntity(domain);
        then(messageRepository).should().save(entity);
        then(mapper).should(never()).toDomain(any());
    }

    @Test
    void update_shouldUpdateNotificationMessage() {
        // Given
        NotificationMessage domain = createSampleNotificationMessage();
        NotificationMessageEntity entity = createSampleNotificationMessageEntity();

        given(mapper.toEntity(domain)).willReturn(entity);
        given(messageRepository.save(entity)).willReturn(Mono.just(entity));
        given(mapper.toDomain(entity)).willReturn(domain);

        // When & Then
        StepVerifier.create(adapter.update(domain))
                .assertNext(updatedMessage -> {
                    assertThat(updatedMessage).isNotNull();
                    assertThat(updatedMessage.getMessageId()).isEqualTo(domain.getMessageId());
                    assertThat(updatedMessage.getNotificationType()).isEqualTo(domain.getNotificationType());
                    assertThat(updatedMessage.getDeliveryStatus()).isEqualTo(domain.getDeliveryStatus());
                })
                .verifyComplete();

        then(mapper).should().toEntity(domain);
        then(messageRepository).should().save(entity);
        then(mapper).should().toDomain(entity);
    }

    @Test
    void update_shouldThrowDataNotFoundExceptionWhenNoResult() {
        // Given
        NotificationMessage domain = createSampleNotificationMessage();
        NotificationMessageEntity entity = createSampleNotificationMessageEntity();

        given(mapper.toEntity(domain)).willReturn(entity);
        given(messageRepository.save(entity)).willReturn(Mono.empty());

        // When & Then
        StepVerifier.create(adapter.update(domain))
                .expectError(DataNotFoundException.class)
                .verify();

        then(mapper).should().toEntity(domain);
        then(messageRepository).should().save(entity);
        then(mapper).should(never()).toDomain(any());
    }

    @Test
    void update_shouldHandleMapperException() {
        // Given
        NotificationMessage domain = createSampleNotificationMessage();
        RuntimeException exception = new RuntimeException("Mapping error");

        given(mapper.toEntity(domain)).willThrow(exception);

        // When & Then
        StepVerifier.create(adapter.update(domain))
                .expectError(RuntimeException.class)
                .verify();

        then(mapper).should().toEntity(domain);
        then(messageRepository).should(never()).save(any());
    }

    @Test
    void findById_shouldReturnNotificationMessage() {
        // Given
        NotificationMessageId messageId = new NotificationMessageId("msg-123");
        NotificationMessage domain = createSampleNotificationMessage();
        NotificationMessageEntity entity = createSampleNotificationMessageEntity();

        given(messageRepository.findById(messageId.value())).willReturn(Mono.just(entity));
        given(mapper.toDomain(entity)).willReturn(domain);

        // When & Then
        StepVerifier.create(adapter.findById(messageId))
                .assertNext(foundMessage -> {
                    assertThat(foundMessage).isNotNull();
                    assertThat(foundMessage.getMessageId()).isEqualTo(domain.getMessageId());
                    assertThat(foundMessage.getNotificationType()).isEqualTo(domain.getNotificationType());
                })
                .verifyComplete();

        then(messageRepository).should().findById(messageId.value());
        then(mapper).should().toDomain(entity);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        // Given
        NotificationMessageId messageId = new NotificationMessageId("non-existent-id");

        given(messageRepository.findById(messageId.value())).willReturn(Mono.empty());

        // When & Then
        StepVerifier.create(adapter.findById(messageId))
                .expectComplete()
                .verify();

        then(messageRepository).should().findById(messageId.value());
        then(mapper).should(never()).toDomain(any());
    }

    @Test
    void findById_shouldHandleRepositoryException() {
        // Given
        NotificationMessageId messageId = new NotificationMessageId("msg-123");
        RuntimeException exception = new RuntimeException("Database error");

        given(messageRepository.findById(messageId.value())).willReturn(Mono.error(exception));

        // When & Then
        StepVerifier.create(adapter.findById(messageId))
                .expectError(RuntimeException.class)
                .verify();

        then(messageRepository).should().findById(messageId.value());
        then(mapper).should(never()).toDomain(any());
    }

    @Test
    void findById_shouldHandleMapperException() {
        // Given
        NotificationMessageId messageId = new NotificationMessageId("msg-123");
        NotificationMessageEntity entity = createSampleNotificationMessageEntity();
        RuntimeException exception = new RuntimeException("Mapping error");

        given(messageRepository.findById(messageId.value())).willReturn(Mono.just(entity));
        given(mapper.toDomain(entity)).willThrow(exception);

        // When & Then
        StepVerifier.create(adapter.findById(messageId))
                .expectError(RuntimeException.class)
                .verify();

        then(messageRepository).should().findById(messageId.value());
        then(mapper).should().toDomain(entity);
    }

    @Test
    void deleteById_shouldDeleteNotificationMessage() {
        // Given
        NotificationMessageId messageId = new NotificationMessageId("msg-123");

        given(messageRepository.deleteById(messageId.value())).willReturn(Mono.empty());

        // When & Then
        StepVerifier.create(adapter.deleteById(messageId))
                .expectComplete()
                .verify();

        then(messageRepository).should().deleteById(messageId.value());
    }

    @Test
    void deleteById_shouldHandleRepositoryException() {
        // Given
        NotificationMessageId messageId = new NotificationMessageId("msg-123");
        RuntimeException exception = new RuntimeException("Database error");

        given(messageRepository.deleteById(messageId.value())).willReturn(Mono.error(exception));

        // When & Then
        StepVerifier.create(adapter.deleteById(messageId))
                .expectError(RuntimeException.class)
                .verify();

        then(messageRepository).should().deleteById(messageId.value());
    }

    private NotificationMessage createSampleNotificationMessage() {
        return new NotificationMessage(
                new NotificationMessageId("msg-123"),
                new NotificationRequestId("req-123"),
                NotificationType.EMAIL,
                new Recipient("user-123", "test@example.com", "010-1234-5678", "device-token", "ko"),
                new NotificationContent("Test Title", "Test Body", "http://example.com",
                        "http://example.com/image.jpg"),
                new EmailSender("sender-123", "sender@example.com", "Test Sender"),
                DeliveryStatus.PENDING,
                Instant.now(),
                null,
                null,
                Instant.now());
    }

    private NotificationMessageEntity createSampleNotificationMessageEntity() {
        return NotificationMessageEntity.builder()
                .messageId("msg-123")
                .requestId("req-123")
                .notificationType("EMAIL")
                .userId("user-123")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .deviceToken("device-token")
                .language("ko")
                .title("Test Title")
                .body("Test Body")
                .redirectUrl("http://example.com")
                .imageUrl("http://example.com/image.jpg")
                .senderId("sender-123")
                .senderEmailAddress("sender@example.com")
                .senderName("Test Sender")
                .deliveryStatus("PENDING")
                .scheduledAt(null)
                .dispatchedAt(null)
                .failureReason(null)
                .createdAt(null)
                .build();
    }

}
