package notification.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import notification.domain.enums.DeliveryStatus;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.sender.SenderInfo;
import notification.domain.vo.sender.SmsSender;

@DisplayName("NotificationMessage 도메인 테스트")
public class NotificationMessageTest {

    private NotificationRequestId requestId;
    private NotificationType notificationType;
    private Recipient recipient;
    private NotificationContent notificationContent;
    private SenderInfo senderInfo;
    private Instant scheduledAt;

    @BeforeEach
    void setUp() {
        requestId = NotificationRequestId.create();
        notificationType = NotificationType.SMS;
        recipient = new Recipient("user123", "test@example.com", "01012345678", "device-token", "ko");
        notificationContent = new NotificationContent("제목", "내용", "https://example.com", "https://image.com");
        senderInfo = new SmsSender("sender-id-1", "01000000000", "테스트 발신자");
        scheduledAt = Instant.now().plusSeconds(3600);
    }

    @DisplayName("NotificationMessage 생성 성공")
    @Test
    void create_success() {
        // when
        NotificationMessage message = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);

        // then
        assertNotNull(message);
        assertEquals(requestId, message.getRequestId());
        assertEquals(notificationType, message.getNotificationType());
        assertEquals(recipient, message.getRecipient());
        assertEquals(notificationContent, message.getNotificationContent());
        assertEquals(senderInfo, message.getSenderInfo());
        assertEquals(scheduledAt, message.getScheduledAt());
        assertEquals(DeliveryStatus.PENDING, message.getDeliveryStatus());
        assertNull(message.getDispatchedAt());
        assertNull(message.getFailureReason());
        assertNull(message.getCreatedAt());
        assertNotNull(message.getMessageId());
    }

    @DisplayName("PENDING 상태에서 DISPATCHED로 상태 변경 성공")
    @Test
    void markAsDispatched_fromPending_success() {
        // given
        NotificationMessage message = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);

        // when
        message.markAsDispatched();

        // then
        assertEquals(DeliveryStatus.DISPATCHED, message.getDeliveryStatus());
        assertNotNull(message.getDispatchedAt());
        assertTrue(message.getDispatchedAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @DisplayName("PENDING이 아닌 상태에서 markAsDispatched 호출 시 예외 발생")
    @Test
    void markAsDispatched_fromNonPending_throwsException() {
        // given
        NotificationMessage message = createMessageWithStatus(DeliveryStatus.DISPATCHED);

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                message::markAsDispatched);
        assertEquals("Cannot mark as dispatched when status is not PENDING", exception.getMessage());
    }

    @DisplayName("PENDING 상태에서 FAILED로 상태 변경 성공")
    @Test
    void markAsFailed_fromPending_success() {
        // given
        NotificationMessage message = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);
        String failureReason = "네트워크 오류";

        // when
        message.markAsFailed(failureReason);

        // then
        assertEquals(DeliveryStatus.FAILED, message.getDeliveryStatus());
        assertEquals(failureReason, message.getFailureReason());
        assertNotNull(message.getDispatchedAt());
        assertTrue(message.getDispatchedAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @DisplayName("DISPATCHED 상태에서 FAILED로 상태 변경 성공")
    @Test
    void markAsFailed_fromDispatched_success() {
        // given
        NotificationMessage message = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);
        message.markAsDispatched();
        String failureReason = "외부 서비스 오류";

        // when
        message.markAsFailed(failureReason);

        // then
        assertEquals(DeliveryStatus.FAILED, message.getDeliveryStatus());
        assertEquals(failureReason, message.getFailureReason());
        assertNotNull(message.getDispatchedAt());
    }

    @DisplayName("FAILED 상태에서 markAsFailed 호출 시 예외 발생")
    @Test
    void markAsFailed_fromFailed_throwsException() {
        // given
        NotificationMessage message = createMessageWithStatus(DeliveryStatus.FAILED);

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> message.markAsFailed("실패 사유"));
        assertEquals("Cannot mark as failed when status is not PENDING or DISPATCHED",
                exception.getMessage());
    }

    @DisplayName("생성자로 모든 필드를 포함한 NotificationMessage 생성")
    @Test
    void constructor_withAllFields_success() {
        // given
        NotificationMessageId messageId = NotificationMessageId.create();
        DeliveryStatus deliveryStatus = DeliveryStatus.DISPATCHED;
        Instant dispatchedAt = Instant.now().minusSeconds(60);
        String failureReason = "테스트 실패 사유";
        Instant createdAt = Instant.now().minusSeconds(120);

        // when
        NotificationMessage message = new NotificationMessage(
                messageId, requestId, notificationType, recipient, notificationContent,
                senderInfo, deliveryStatus, scheduledAt, dispatchedAt, failureReason, createdAt);

        // then
        assertEquals(messageId, message.getMessageId());
        assertEquals(requestId, message.getRequestId());
        assertEquals(notificationType, message.getNotificationType());
        assertEquals(recipient, message.getRecipient());
        assertEquals(notificationContent, message.getNotificationContent());
        assertEquals(senderInfo, message.getSenderInfo());
        assertEquals(deliveryStatus, message.getDeliveryStatus());
        assertEquals(scheduledAt, message.getScheduledAt());
        assertEquals(dispatchedAt, message.getDispatchedAt());
        assertEquals(failureReason, message.getFailureReason());
        assertEquals(createdAt, message.getCreatedAt());
    }

    @DisplayName("create 메소드로 생성된 메시지의 초기 상태 검증")
    @Test
    void create_initialState_verification() {
        // when
        NotificationMessage message = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);

        // then
        assertEquals(DeliveryStatus.PENDING, message.getDeliveryStatus());
        assertNull(message.getDispatchedAt());
        assertNull(message.getFailureReason());
        assertNull(message.getCreatedAt());
        assertNotNull(message.getMessageId());
    }

    @DisplayName("상태 변경 시퀀스 테스트 - PENDING -> DISPATCHED -> FAILED")
    @Test
    void statusTransitionSequence_success() {
        // given
        NotificationMessage message = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);

        // when & then - PENDING -> DISPATCHED
        assertEquals(DeliveryStatus.PENDING, message.getDeliveryStatus());
        message.markAsDispatched();
        assertEquals(DeliveryStatus.DISPATCHED, message.getDeliveryStatus());
        assertNotNull(message.getDispatchedAt());

        // when & then - DISPATCHED -> FAILED
        String failureReason = "전송 실패";
        message.markAsFailed(failureReason);
        assertEquals(DeliveryStatus.FAILED, message.getDeliveryStatus());
        assertEquals(failureReason, message.getFailureReason());
    }

    @DisplayName("scheduledAt이 null인 경우에도 정상 생성됨")
    @Test
    void create_withNullScheduledAt_success() {
        // when
        NotificationMessage message = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, null);

        // then
        assertNotNull(message);
        assertNull(message.getScheduledAt());
        assertEquals(DeliveryStatus.PENDING, message.getDeliveryStatus());
    }

    @DisplayName("여러 타입의 NotificationType으로 메시지 생성")
    @Test
    void create_withDifferentNotificationTypes() {
        // SMS
        NotificationMessage smsMessage = NotificationMessage.create(
                requestId, NotificationType.SMS, recipient, notificationContent, senderInfo,
                scheduledAt);
        assertEquals(NotificationType.SMS, smsMessage.getNotificationType());

        // EMAIL
        NotificationMessage emailMessage = NotificationMessage.create(
                requestId, NotificationType.EMAIL, recipient, notificationContent, senderInfo,
                scheduledAt);
        assertEquals(NotificationType.EMAIL, emailMessage.getNotificationType());

        // PUSH
        NotificationMessage pushMessage = NotificationMessage.create(
                requestId, NotificationType.PUSH, recipient, notificationContent, senderInfo,
                scheduledAt);
        assertEquals(NotificationType.PUSH, pushMessage.getNotificationType());
    }

    @DisplayName("markAsDispatched 호출 시 dispatchedAt이 현재 시간으로 설정됨")
    @Test
    void markAsDispatched_setsDispatchedAtToCurrentTime() {
        // given
        NotificationMessage message = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);
        Instant before = Instant.now();

        // when
        message.markAsDispatched();

        // then
        Instant after = Instant.now();
        assertNotNull(message.getDispatchedAt());
        assertTrue(message.getDispatchedAt().isAfter(before.minusSeconds(1)));
        assertTrue(message.getDispatchedAt().isBefore(after.plusSeconds(1)));
    }

    @DisplayName("markAsFailed 호출 시 dispatchedAt이 현재 시간으로 설정됨")
    @Test
    void markAsFailed_setsDispatchedAtToCurrentTime() {
        // given
        NotificationMessage message = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);
        Instant before = Instant.now();

        // when
        message.markAsFailed("실패 사유");

        // then
        Instant after = Instant.now();
        assertNotNull(message.getDispatchedAt());
        assertTrue(message.getDispatchedAt().isAfter(before.minusSeconds(1)));
        assertTrue(message.getDispatchedAt().isBefore(after.plusSeconds(1)));
    }

    @DisplayName("messageId가 고유하게 생성됨")
    @Test
    void create_generatesUniqueMessageId() {
        // when
        NotificationMessage message1 = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);
        NotificationMessage message2 = NotificationMessage.create(
                requestId, notificationType, recipient, notificationContent, senderInfo, scheduledAt);

        // then
        assertNotNull(message1.getMessageId());
        assertNotNull(message2.getMessageId());
        assertNotEquals(message1.getMessageId(), message2.getMessageId());
    }

    @DisplayName("생성자의 모든 필드가 올바르게 설정됨")
    @Test
    void constructor_allFieldsSetCorrectly() {
        // given
        NotificationMessageId messageId = NotificationMessageId.create();
        DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;
        Instant dispatchedAt = null;
        String failureReason = null;
        Instant createdAt = Instant.now().minusSeconds(60);

        // when
        NotificationMessage message = new NotificationMessage(
                messageId, requestId, notificationType, recipient, notificationContent,
                senderInfo, deliveryStatus, scheduledAt, dispatchedAt, failureReason, createdAt);

        // then
        assertEquals(messageId, message.getMessageId());
        assertEquals(requestId, message.getRequestId());
        assertEquals(notificationType, message.getNotificationType());
        assertEquals(recipient, message.getRecipient());
        assertEquals(notificationContent, message.getNotificationContent());
        assertEquals(senderInfo, message.getSenderInfo());
        assertEquals(scheduledAt, message.getScheduledAt());
        assertEquals(deliveryStatus, message.getDeliveryStatus());
        assertEquals(dispatchedAt, message.getDispatchedAt());
        assertEquals(failureReason, message.getFailureReason());
        assertEquals(createdAt, message.getCreatedAt());
    }

    private NotificationMessage createMessageWithStatus(DeliveryStatus status) {
        NotificationMessageId messageId = NotificationMessageId.create();
        Instant now = Instant.now();

        return new NotificationMessage(
                messageId, requestId, notificationType, recipient, notificationContent,
                senderInfo, status, scheduledAt,
                status == DeliveryStatus.PENDING ? null : now.minusSeconds(60),
                status == DeliveryStatus.FAILED ? "테스트 실패" : null,
                now.minusSeconds(120));
    }
}
