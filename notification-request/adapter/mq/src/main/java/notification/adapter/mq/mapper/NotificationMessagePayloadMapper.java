package notification.adapter.mq.mapper;

import org.springframework.stereotype.Component;

import notification.adapter.mq.payload.EmailMessagePayload;
import notification.adapter.mq.payload.NotificationMessagePayload;
import notification.adapter.mq.payload.PushMessagePayload;
import notification.adapter.mq.payload.SmsMessagePayload;
import notification.domain.NotificationMessage;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SmsSender;
import reactor.core.publisher.Mono;

@Component
public class NotificationMessagePayloadMapper {

    public Mono<NotificationMessagePayload> toPayload(NotificationMessage domain) {
        return Mono.justOrEmpty(domain).map(message -> {
            switch (message.getNotificationType()) {
                case EMAIL:
                    return toEmailPayload(message);
                case SMS:
                    return toSmsPayload(message);
                case PUSH:
                    return toPushPayload(message);
                default:
                    throw new IllegalArgumentException(
                            "Unknown notification type: " + message.getNotificationType());
            }
        });
    }

    private EmailMessagePayload toEmailPayload(NotificationMessage message) {
        EmailSender senderInfo = (EmailSender) message.getSenderInfo();

        return EmailMessagePayload.builder()
                .messageId(message.getMessageId().value())
                .requestId(message.getRequestId().value())
                .createdAt(message.getCreatedAt())
                .subject(message.getNotificationContent().title())
                .body(message.getNotificationContent().body())
                .senderEmail(senderInfo.senderEmailAddress())
                .senderName(senderInfo.senderName())
                .recipientEmail(message.getRecipient().email())
                .build();
    }

    private SmsMessagePayload toSmsPayload(NotificationMessage message) {
        SmsSender senderInfo = (SmsSender) message.getSenderInfo();

        return SmsMessagePayload.builder()
                .messageId(message.getMessageId().value())
                .requestId(message.getRequestId().value())
                .createdAt(message.getCreatedAt())
                .senderPhone(senderInfo.senderPhoneNumber())
                .recipientPhone(message.getRecipient().phoneNumber())
                .messageText(message.getNotificationContent().body())
                .build();
    }

    private PushMessagePayload toPushPayload(NotificationMessage message) {
        PushSender senderInfo = (PushSender) message.getSenderInfo();

        return PushMessagePayload.builder()
                .messageId(message.getMessageId().value())
                .requestId(message.getRequestId().value())
                .createdAt(message.getCreatedAt())
                .deviceToken(message.getRecipient().deviceToken())
                .title(message.getNotificationContent().title())
                .body(message.getNotificationContent().body())
                .imageUrl(message.getNotificationContent().imageUrl())
                .redirectUrl(message.getNotificationContent().redirectUrl())
                .senderName(senderInfo.senderName())
                .build();
    }

}
