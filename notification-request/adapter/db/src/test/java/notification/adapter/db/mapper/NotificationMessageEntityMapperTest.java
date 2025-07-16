package notification.adapter.db.mapper;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import notification.adapter.db.NotificationMessageEntity;
import notification.domain.NotificationMessage;
import notification.domain.enums.DeliveryStatus;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SmsSender;

class NotificationMessageEntityMapperTest {

    private NotificationMessageEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NotificationMessageEntityMapper();
    }

    @Test
    void testFromDomainAndToDomain_email() {
        NotificationMessage domain = new NotificationMessage(
                new NotificationMessageId("msg-1"),
                new NotificationRequestId("req-1"),
                NotificationType.EMAIL,
                new Recipient("user-1", "email@ex.com", "010-1111-2222", "dev-token", "ko"),
                new NotificationContent("title", "body", "url", "img"),
                new EmailSender("sender-1", "email@ex.com", "SenderName"),
                DeliveryStatus.PENDING,
                Instant.now(),
                Instant.now(),
                null,
                Instant.now());

        NotificationMessageEntity entity = mapper.toEntity(domain);
        NotificationMessage result = mapper.toDomain(entity);

        assertThat(result.getMessageId().value()).isEqualTo(domain.getMessageId().value());
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.EMAIL);
        assertThat(result.getSenderInfo()).isInstanceOf(EmailSender.class);
        assertThat(result.getRecipient().email()).isEqualTo("email@ex.com");
    }

    @Test
    void testFromDomainAndToDomain_sms() {
        NotificationMessage domain = new NotificationMessage(
                new NotificationMessageId("msg-2"),
                new NotificationRequestId("req-2"),
                NotificationType.SMS,
                new Recipient("user-2", "", "010-3333-4444", null, null),
                new NotificationContent("title2", "body2", "url2", "img2"),
                new SmsSender("sender-2", "010-3333-4444", "SmsSender"),
                DeliveryStatus.DISPATCHED,
                Instant.now(),
                Instant.now(),
                null,
                Instant.now());

        NotificationMessageEntity entity = mapper.toEntity(domain);
        NotificationMessage result = mapper.toDomain(entity);

        assertThat(result.getNotificationType()).isEqualTo(NotificationType.SMS);
        assertThat(result.getSenderInfo()).isInstanceOf(SmsSender.class);
        assertThat(result.getRecipient().phoneNumber()).isEqualTo("010-3333-4444");
    }

    @Test
    void testFromDomainAndToDomain_push() {
        NotificationMessage domain = new NotificationMessage(
                new NotificationMessageId("msg-3"),
                new NotificationRequestId("req-3"),
                NotificationType.PUSH,
                new Recipient("user-3", null, null, "push-token", "en"),
                new NotificationContent("title3", "body3", "url3", "img3"),
                new PushSender("sender-3", "PushSenderName"),
                DeliveryStatus.FAILED,
                Instant.now(),
                Instant.now(),
                "fail reason",
                Instant.now());

        NotificationMessageEntity entity = mapper.toEntity(domain);
        NotificationMessage result = mapper.toDomain(entity);

        assertThat(result.getNotificationType()).isEqualTo(NotificationType.PUSH);
        assertThat(result.getSenderInfo()).isInstanceOf(PushSender.class);
        assertThat(result.getRecipient().deviceToken()).isEqualTo("push-token");
        assertThat(result.getFailureReason()).isEqualTo("fail reason");
    }

}
