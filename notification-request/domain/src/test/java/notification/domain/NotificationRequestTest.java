package notification.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import notification.domain.enums.NotificationType;
import notification.domain.enums.RequestStatus;
import notification.domain.enums.RequesterType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Requester;
import notification.domain.vo.TemplateInfo;
import notification.domain.vo.UserId;
import notification.domain.vo.recipient.RecipientReference;
import notification.domain.vo.recipient.UserRecipient;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SenderInfo;

@DisplayName("NotificationRequest 도메인 테스트")
public class NotificationRequestTest {

    private Requester requester;
    private List<RecipientReference> recipients;
    private List<NotificationType> notificationTypes;
    private Map<NotificationType, SenderInfo> senderInfos;
    private NotificationContent content;
    private TemplateInfo template;
    private String memo;
    private Instant scheduledAt;

    @BeforeEach
    void setUp() {
        requester = new Requester(RequesterType.SERVICE, "test-service");
        recipients = List.of(new UserRecipient(UserId.of("user-001")));
        notificationTypes = List.of(NotificationType.EMAIL, NotificationType.PUSH);
        senderInfos = Map.of(
                NotificationType.EMAIL, new EmailSender("test@example.com", "Test Sender"),
                NotificationType.PUSH, new PushSender("Test App"));
        content = new NotificationContent("Test Content", "Test Subject", null, null);
        template = new TemplateInfo("TEMPLATE_001", Map.of("key", "value"));
        memo = "Test memo";
        scheduledAt = Instant.now().plusSeconds(3600);
    }

    @Test
    @DisplayName("NotificationRequest 생성 팩토리 메서드가 정상 동작한다")
    void create_shouldCreateNotificationRequestWithDefaultValues() {
        // when
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);

        // then
        assertNotNull(request.getRequestId());
        assertEquals(requester, request.getRequester());
        assertEquals(recipients, request.getRecipients());
        assertEquals(notificationTypes, request.getNotificationTypes());
        assertEquals(senderInfos, request.getSenderInfos());
        assertEquals(content, request.getContent());
        assertEquals(template, request.getTemplate());
        assertEquals(memo, request.getMemo());
        assertEquals(scheduledAt, request.getScheduledAt());
        assertEquals(RequestStatus.PENDING, request.getStatus());
        assertNull(request.getFailureReason());
        assertNull(request.getProcessedAt());
        assertNull(request.getCreatedAt());
    }

    @Test
    @DisplayName("PENDING 상태에서 PROCESSING으로 상태 변경이 가능하다")
    void markAsProcessing_shouldChangeStatusFromPendingToProcessing() {
        // given
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);

        // when
        request.markAsProcessing();

        // then
        assertEquals(RequestStatus.PROCESSING, request.getStatus());
        assertNotNull(request.getProcessedAt());
    }

    @Test
    @DisplayName("PENDING이 아닌 상태에서 PROCESSING으로 변경 시 예외가 발생한다")
    void markAsProcessing_shouldThrowExceptionWhenStatusIsNotPending() {
        // given
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);
        request.markAsProcessing(); // PROCESSING 상태로 변경

        // when & then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                request::markAsProcessing);
        assertEquals("Cannot mark as processed when status is not PENDING", exception.getMessage());
    }

    @Test
    @DisplayName("PROCESSING 상태에서 DISPATCHED로 상태 변경이 가능하다")
    void markAsDispatched_shouldChangeStatusFromProcessingToDispatched() {
        // given
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);
        request.markAsProcessing();
        Instant beforeDispatched = request.getProcessedAt();

        // when
        request.markAsDispatched();

        // then
        assertEquals(RequestStatus.DISPATCHED, request.getStatus());
        assertTrue(request.getProcessedAt().isAfter(beforeDispatched) ||
                request.getProcessedAt().equals(beforeDispatched));
    }

    @Test
    @DisplayName("PROCESSING이 아닌 상태에서 DISPATCHED로 변경 시 예외가 발생한다")
    void markAsDispatched_shouldThrowExceptionWhenStatusIsNotProcessing() {
        // given
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);

        // when & then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                request::markAsDispatched);
        assertEquals("Cannot mark as dispatched when status is not PROCESSING", exception.getMessage());
    }

    @Test
    @DisplayName("PROCESSING 상태에서 FAILED로 상태 변경이 가능하다")
    void markAsFailed_shouldChangeStatusFromProcessingToFailed() {
        // given
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);
        request.markAsProcessing();
        String failureReason = "Template not found";

        // when
        request.markAsFailed(failureReason);

        // then
        assertEquals(RequestStatus.FAILED, request.getStatus());
        assertEquals(failureReason, request.getFailureReason());
        assertNotNull(request.getProcessedAt());
    }

    @Test
    @DisplayName("PROCESSING이 아닌 상태에서 FAILED로 변경 시 예외가 발생한다")
    void markAsFailed_shouldThrowExceptionWhenStatusIsNotProcessing() {
        // given
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);

        // when & then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> request.markAsFailed("Some reason"));
        assertEquals("Cannot mark as failed when status is not PROCESSING", exception.getMessage());
    }

    @Test
    @DisplayName("PENDING 상태에서 CANCELED로 상태 변경이 가능하다")
    void markAsCanceled_shouldChangeStatusFromPendingToCanceled() {
        // given
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);

        // when
        request.markAsCanceled();

        // then
        assertEquals(RequestStatus.CANCELED, request.getStatus());
        assertNotNull(request.getProcessedAt());
    }

    @Test
    @DisplayName("PROCESSING 상태에서 CANCELED로 상태 변경이 가능하다")
    void markAsCanceled_shouldChangeStatusFromProcessingToCanceled() {
        // given
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);
        request.markAsProcessing();

        // when
        request.markAsCanceled();

        // then
        assertEquals(RequestStatus.CANCELED, request.getStatus());
        assertNotNull(request.getProcessedAt());
    }

    @Test
    @DisplayName("DISPATCHED 상태에서 CANCELED로 변경 시 예외가 발생한다")
    void markAsCanceled_shouldThrowExceptionWhenStatusIsDispatched() {
        // given
        NotificationRequest request = NotificationRequest.create(
                requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt);
        request.markAsProcessing();
        request.markAsDispatched();

        // when & then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                request::markAsCanceled);
        assertEquals("Cannot mark as cancelled when status is not PENDING or PROCESSING",
                exception.getMessage());
    }

    @Test
    @DisplayName("생성자로 모든 필드를 설정하여 객체를 생성할 수 있다")
    void constructor_shouldCreateNotificationRequestWithAllFields() {
        // given
        NotificationRequestId requestId = NotificationRequestId.create();
        RequestStatus status = RequestStatus.PROCESSING;
        String failureReason = "Test failure";
        Instant processedAt = Instant.now();
        Instant createdAt = Instant.now().minusSeconds(3600);

        // when
        NotificationRequest request = new NotificationRequest(
                requestId, requester, recipients, notificationTypes, senderInfos,
                content, template, memo, scheduledAt, status, failureReason, processedAt, createdAt);

        // then
        assertEquals(requestId, request.getRequestId());
        assertEquals(requester, request.getRequester());
        assertEquals(recipients, request.getRecipients());
        assertEquals(notificationTypes, request.getNotificationTypes());
        assertEquals(senderInfos, request.getSenderInfos());
        assertEquals(content, request.getContent());
        assertEquals(template, request.getTemplate());
        assertEquals(memo, request.getMemo());
        assertEquals(scheduledAt, request.getScheduledAt());
        assertEquals(status, request.getStatus());
        assertEquals(failureReason, request.getFailureReason());
        assertEquals(processedAt, request.getProcessedAt());
        assertEquals(createdAt, request.getCreatedAt());
    }
}
