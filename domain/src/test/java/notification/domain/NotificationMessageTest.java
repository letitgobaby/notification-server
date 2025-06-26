package notification.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import notification.definition.enums.DeliveryStatus;
import notification.definition.enums.NotificationType;
import notification.definition.exceptions.MandatoryFieldException;
import notification.definition.exceptions.PolicyViolationException;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Recipient;
import notification.domain.vo.SenderInfo;

class NotificationMessageTest {

        private NotificationRequestId requestId;
        private NotificationType notificationType;
        private Recipient recipient;
        private NotificationContent content;
        private SenderInfo senderInfo;
        private Instant scheduledAt;

        @BeforeEach
        void setUp() {
                requestId = NotificationRequestId.of("req-123");
                notificationType = NotificationType.EMAIL;
                recipient = Recipient.ofPush("device-token");
                content = new NotificationContent("Test Title", "Test Body",
                                "http://example.com", "http://example.com/image.png");
                senderInfo = SenderInfo.ofPushSender("Test Sender");
                scheduledAt = Instant.now();
        }

        @Test
        void create_shouldReturnPendingNotificationMessage() {
                NotificationMessage message = NotificationMessage.create(
                                requestId, notificationType, recipient, content, senderInfo, scheduledAt);

                assertNotNull(message.getItemId());
                assertEquals(requestId, message.getNotificationRequestId());
                assertEquals(notificationType, message.getNotificationType());
                assertEquals(recipient, message.getRecipient());
                assertEquals(content, message.getNotificationContent());
                assertEquals(senderInfo, message.getSenderInfo());
                assertEquals(DeliveryStatus.PENDING, message.getDeliveryStatus());
                assertEquals(scheduledAt, message.getScheduledAt());
                assertNull(message.getDispatchedAt());
                assertNull(message.getFailureReason());
        }

        @Test
        void constructor_shouldThrowMandatoryFieldNullException_whenRequiredFieldIsNull() {
                assertThrows(MandatoryFieldException.class, () -> new NotificationMessage(
                                null, requestId, notificationType, recipient, content, senderInfo,
                                DeliveryStatus.PENDING, scheduledAt, null, null));
                assertThrows(MandatoryFieldException.class, () -> new NotificationMessage(
                                NotificationMessageId.generate(), null, notificationType, recipient, content,
                                senderInfo,
                                DeliveryStatus.PENDING, scheduledAt, null, null));
                assertThrows(MandatoryFieldException.class, () -> new NotificationMessage(
                                NotificationMessageId.generate(), requestId, null, recipient, content, senderInfo,
                                DeliveryStatus.PENDING, scheduledAt, null, null));
                assertThrows(MandatoryFieldException.class, () -> new NotificationMessage(
                                NotificationMessageId.generate(), requestId, notificationType, null, content,
                                senderInfo,
                                DeliveryStatus.PENDING, scheduledAt, null, null));
                assertThrows(MandatoryFieldException.class, () -> new NotificationMessage(
                                NotificationMessageId.generate(), requestId, notificationType, recipient, null,
                                senderInfo,
                                DeliveryStatus.PENDING, scheduledAt, null, null));
                assertThrows(MandatoryFieldException.class, () -> new NotificationMessage(
                                NotificationMessageId.generate(), requestId, notificationType, recipient, content, null,
                                DeliveryStatus.PENDING, scheduledAt, null, null));
                assertThrows(MandatoryFieldException.class, () -> new NotificationMessage(
                                NotificationMessageId.generate(), requestId, notificationType, recipient, content,
                                senderInfo,
                                null, scheduledAt, null, null));
        }

        @Test
        void markAsDispatched_shouldChangeStatusToDispatchedAndSetDispatchedAt() {
                NotificationMessage message = NotificationMessage.create(
                                requestId, notificationType, recipient, content, senderInfo, scheduledAt);

                Instant dispatchedAt = Instant.now();
                message.markAsDispatched(dispatchedAt);

                assertEquals(DeliveryStatus.DISPATCHED, message.getDeliveryStatus());
                assertEquals(dispatchedAt, message.getDispatchedAt());
        }

        @Test
        void markAsDispatched_shouldThrowExceptionIfNotPending() {
                NotificationMessage message = NotificationMessage.create(
                                requestId, notificationType, recipient, content, senderInfo, scheduledAt);

                message.markAsDispatched(Instant.now());
                assertThrows(PolicyViolationException.class, () -> message.markAsDispatched(Instant.now()));
        }

        @Test
        void markAsDispatched_shouldThrowExceptionIfDispatchedAtIsNull() {
                NotificationMessage message = NotificationMessage.create(
                                requestId, notificationType, recipient, content, senderInfo, scheduledAt);

                assertThrows(NullPointerException.class, () -> message.markAsDispatched(null));
        }

        @Test
        void markAsFailed_shouldChangeStatusToFailedAndSetFailureReasonAndDispatchedAt() {
                NotificationMessage message = NotificationMessage.create(
                                requestId, notificationType, recipient, content, senderInfo, scheduledAt);

                message.markAsFailed("Network error");

                assertEquals(DeliveryStatus.FAILED, message.getDeliveryStatus());
                assertEquals("Network error", message.getFailureReason());
                assertNotNull(message.getDispatchedAt());
        }

        @Test
        void markAsFailed_shouldThrowExceptionIfNotPendingOrDispatched() {
                NotificationMessage message = NotificationMessage.create(
                                requestId, notificationType, recipient, content, senderInfo, scheduledAt);

                message.markAsFailed("fail");
                assertThrows(PolicyViolationException.class, () -> message.markAsFailed("fail again"));
        }

        @Test
        void markAsFailed_shouldThrowExceptionIfReasonIsNull() {
                NotificationMessage message = NotificationMessage.create(
                                requestId, notificationType, recipient, content, senderInfo, scheduledAt);

                assertThrows(NullPointerException.class, () -> message.markAsFailed(null));
        }

        // @Test
        // void markAsDelivered_shouldChangeStatusToDeliveredAndSetDispatchedAt() {
        // NotificationMessage message = NotificationMessage.create(
        // requestId, notificationType, recipient, content, senderInfo, scheduledAt);

        // message.markAsDispatched(Instant.now());
        // message.markAsDelivered();

        // assertEquals(DeliveryStatus.DELIVERED, message.getDeliveryStatus());
        // assertNotNull(message.getDispatchedAt());
        // }

        // @Test
        // void markAsDelivered_shouldThrowExceptionIfNotDispatched() {
        // NotificationMessage message = NotificationMessage.create(
        // requestId, notificationType, recipient, content, senderInfo, scheduledAt);

        // assertThrows(PolicyViolationException.class, message::markAsDelivered);

        // message.markAsDispatched(Instant.now());
        // message.markAsDelivered();
        // assertThrows(PolicyViolationException.class, message::markAsDelivered);
        // }
}