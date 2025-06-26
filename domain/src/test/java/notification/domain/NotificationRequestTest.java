package notification.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import notification.definition.enums.NotificationType;
import notification.definition.enums.RequestStatus;
import notification.definition.enums.RequesterType;
import notification.definition.exceptions.PolicyViolationException;
import notification.definition.vo.UserId;
import notification.domain.vo.NotificationChannelConfig;
import notification.domain.vo.NotificationRequestDetails;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Recipient;
import notification.domain.vo.Requester;
import notification.domain.vo.SenderInfo;
import notification.domain.vo.TargetAudience;
import notification.domain.vo.TemplateId;

public class NotificationRequestTest {

    private NotificationRequestId requestId;
    private Requester requester;
    private TargetAudience targetAudience;
    private NotificationRequestDetails requestDetails;

    @BeforeEach
    void setUp() {
        requestId = new NotificationRequestId(UUID.randomUUID().toString());
        requester = new Requester(RequesterType.SERVICE, "test-service");
        targetAudience = TargetAudience.mixedAudience(
                Set.of(new UserId("user-1"), new UserId("user-2")),
                List.of(Recipient.ofPush("device-token-1"), Recipient.ofEmail("email-1")) //
        );

        requestDetails = NotificationRequestDetails.forTemplate(
                new TemplateId("TEMPLATE_ID"),
                Map.of("productName", "Test Product", "launchDate", "2025-07-01"),
                List.of(
                        new NotificationChannelConfig(NotificationType.PUSH,
                                SenderInfo.ofPushSender("Test Sender 1")),
                        new NotificationChannelConfig(NotificationType.EMAIL,
                                SenderInfo.ofEmailSender("sender@mail.com",
                                        "Test Sender 2")) //
                ) //
        );
    }

    @Test
    void create_shouldInitializeWithPendingStatus() {
        NotificationRequest request = NotificationRequest.create(
                requestId, requester, targetAudience, requestDetails, null);

        assertNotNull(request);
        assertTrue(request.isPending());
        assertNull(request.getFailureReason());
        assertNull(request.getProcessedAt());
    }

    @Test
    void markAsProcessing_shouldTransitionToProcessing() {
        NotificationRequest request = NotificationRequest.create(
                requestId, requester, targetAudience, requestDetails, null);
        request.markAsProcessing();

        assertTrue(request.isProcessing());
    }

    @Test
    void markAsCompleted_shouldSetProcessedAtAndStatus() {
        NotificationRequest request = NotificationRequest.create(
                requestId, requester, targetAudience, requestDetails, null);
        request.markAsProcessing();
        request.markAsCompleted();

        assertTrue(request.isCompleted());
        assertNotNull(request.getProcessedAt());
    }

    @Test
    void markAsFailed_shouldSetFailureReasonAndStatus() {
        NotificationRequest request = NotificationRequest.create(
                requestId, requester, targetAudience, requestDetails, null);
        request.markAsProcessing();
        request.markAsFailed("Queue unavailable");

        assertTrue(request.isFailed());
        assertEquals("Queue unavailable", request.getFailureReason());
        assertNotNull(request.getProcessedAt());
    }

    @Test
    void markAsCanceled_shouldSetCanceledStatus() {
        NotificationRequest request = NotificationRequest.create(
                requestId, requester, targetAudience, requestDetails, null);
        request.markAsCanceled("User canceled");

        assertTrue(request.isCanceled());
        assertEquals("User canceled", request.getFailureReason());
        assertNotNull(request.getProcessedAt());
    }

    @Test
    void markAsProcessing_shouldThrowIfStatusNotPending() {
        NotificationRequest request = NotificationRequest.create(
                requestId, requester, targetAudience, requestDetails, null);
        request.markAsProcessing();

        PolicyViolationException ex = assertThrows(PolicyViolationException.class, request::markAsProcessing);
        assertTrue(ex.getMessage().contains("unless status is PENDING"));
    }

    @Test
    void markAsCompleted_shouldThrowIfNotProcessing() {
        NotificationRequest request = NotificationRequest.create(
                requestId, requester, targetAudience, requestDetails, null);

        PolicyViolationException ex = assertThrows(PolicyViolationException.class, request::markAsCompleted);
        assertTrue(ex.getMessage().contains("unless status is PROCESSING"));
    }

    @Test
    void constructor_shouldThrowIfScheduledBeforeRequested() {
        Instant now = Instant.now();
        Instant earlier = now.minusSeconds(60);

        PolicyViolationException ex = assertThrows(PolicyViolationException.class,
                () -> new NotificationRequest(requestId, requester, targetAudience, requestDetails,
                        earlier,
                        RequestStatus.PENDING, null, null, now));

        assertEquals("Scheduled time cannot be before requested time", ex.getMessage());
    }

}
