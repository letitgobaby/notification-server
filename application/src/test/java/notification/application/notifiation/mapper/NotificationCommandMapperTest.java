package notification.application.notifiation.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.domain.notification.NotificationRequest;
import notification.domain.notification.enums.AudienceType;
import notification.domain.notification.enums.NotificationType;
import notification.domain.notification.enums.Priority;
import notification.domain.notification.enums.RequesterType;
import notification.domain.notification.vo.NotificationContent;
import notification.domain.notification.vo.PushContent;
import notification.domain.notification.vo.PushRecipient;
import notification.domain.notification.vo.RequesterId;

class NotificationCommandMapperTest {

    private NotificationType type;
    private RequesterType requesterType;
    private RequesterId requesterId;
    private AudienceType audienceType;
    private PushRecipient recipient;
    private NotificationContent content;
    private Instant scheduledAt;
    private Priority priority;

    @BeforeEach
    void setUp() {
        type = NotificationType.PUSH;
        requesterType = RequesterType.USER;
        requesterId = RequesterId.from("user-123");
        audienceType = AudienceType.INDIVIDUAL;
        recipient = new PushRecipient(List.of("device-token"));
        content = new PushContent(null, null, "title", "body", null, null, null);
        scheduledAt = Instant.now().plusSeconds(3600);
        priority = Priority.HIGH;
    }

    @Test
    void toDomain_shouldMapAllFields_whenAllFieldsProvided() {
        NotificationRequestCommand command = mock(NotificationRequestCommand.class);
        when(command.type()).thenReturn(type);
        when(command.requesterType()).thenReturn(requesterType);
        when(command.requesterId()).thenReturn(requesterId);
        when(command.audienceType()).thenReturn(audienceType);
        when(command.recipient()).thenReturn(recipient);
        when(command.content()).thenReturn(content);
        when(command.scheduledAt()).thenReturn(scheduledAt);
        when(command.priority()).thenReturn(priority);

        NotificationRequest result = NotificationCommandMapper.toDomain(command);

        assertNotNull(result);
        assertNotNull(result.getNotificationId());
        assertEquals(requesterId, result.getRequesterId());
        assertEquals(requesterType, result.getRequesterType());
        assertEquals(type, result.getType());
        assertEquals(audienceType, result.getAudienceType());
        assertEquals(recipient, result.getRecipient());
        assertEquals(content, result.getContent());
        assertEquals(scheduledAt, result.getScheduledAt());
        assertEquals(priority, result.getPriority());
        assertNotNull(result.getRequestedAt());
    }

    @Test
    void toDomain_shouldSetDefaults_whenScheduledAtAndPriorityAreNull() {
        NotificationRequestCommand command = mock(NotificationRequestCommand.class);
        when(command.type()).thenReturn(type);
        when(command.requesterType()).thenReturn(requesterType);
        when(command.requesterId()).thenReturn(requesterId);
        when(command.audienceType()).thenReturn(audienceType);
        when(command.recipient()).thenReturn(recipient);
        when(command.content()).thenReturn(content);
        when(command.scheduledAt()).thenReturn(null); // 예약 발송 시간은 null
        when(command.priority()).thenReturn(null); // 우선순위는 null

        Instant before = Instant.now();
        NotificationRequest result = NotificationCommandMapper.toDomain(command);
        Instant after = Instant.now();

        assertNotNull(result);
        assertNotNull(result.getNotificationId());
        assertEquals(requesterId, result.getRequesterId());
        assertEquals(requesterType, result.getRequesterType());
        assertEquals(type, result.getType());
        assertEquals(audienceType, result.getAudienceType());
        assertEquals(recipient, result.getRecipient());
        assertEquals(content, result.getContent());
        assertTrue(result.getScheduledAt().isAfter(before) && result.getScheduledAt().isBefore(after));
        assertEquals(Priority.LOW, result.getPriority()); // 기본값은 LOW
        assertNotNull(result.getRequestedAt());
        assertTrue(result.getRequestedAt().isAfter(before) && result.getRequestedAt().isBefore(after));
    }

}