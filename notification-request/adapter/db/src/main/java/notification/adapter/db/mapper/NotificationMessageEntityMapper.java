package notification.adapter.db.mapper;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.adapter.db.NotificationMessageEntity;
import notification.adapter.db.NotificationMessageEntity.NotificationMessageEntityBuilder;
import notification.definition.utils.InstantDateTimeBridge;
import notification.domain.NotificationMessage;
import notification.domain.enums.DeliveryStatus;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationMessageId;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SenderInfo;
import notification.domain.vo.sender.SmsSender;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageEntityMapper {

    /**
     * NotificationMessage 도메인 객체를 NotificationMessageEntity로 변환합니다.
     * 
     * @param domain
     * @return
     */
    public NotificationMessageEntity toEntity(NotificationMessage domain) {
        NotificationMessageEntityBuilder entity = NotificationMessageEntity.builder()
                .messageId(domain.getMessageId().value())
                .requestId(domain.getRequestId().value())
                .notificationType(domain.getNotificationType().name())
                .userId(domain.getRecipient().userId())
                .phoneNumber(domain.getRecipient().phoneNumber())
                .email(domain.getRecipient().email())
                .deviceToken(domain.getRecipient().deviceToken())
                .language(domain.getRecipient().language())
                .title(domain.getNotificationContent().title())
                .body(domain.getNotificationContent().body())
                .redirectUrl(domain.getNotificationContent().redirectUrl())
                .imageUrl(domain.getNotificationContent().imageUrl())
                .deliveryStatus(domain.getDeliveryStatus().name())
                .scheduledAt(InstantDateTimeBridge.toLocalDateTime(domain.getScheduledAt()))
                .dispatchedAt(InstantDateTimeBridge.toLocalDateTime(domain.getDispatchedAt()))
                .failureReason(domain.getFailureReason())
                .createdAt(InstantDateTimeBridge.toLocalDateTime(domain.getCreatedAt()));

        if (domain.getSenderInfo() instanceof EmailSender emailSender) {
            entity.senderId(emailSender.senderId());
            entity.senderEmailAddress(emailSender.senderEmailAddress());
        } else if (domain.getSenderInfo() instanceof PushSender pushSender) {
            entity.senderId(pushSender.senderId());
            entity.senderName(pushSender.senderName());
        } else if (domain.getSenderInfo() instanceof SmsSender smsSender) {
            entity.senderId(smsSender.senderId());
            entity.senderPhoneNumber(smsSender.senderPhoneNumber());
        }

        return entity.build();
    }

    /**
     * NotificationMessageEntity를 NotificationMessage 도메인 객체로 변환합니다.
     * 
     * @param entity
     * @return
     */
    public NotificationMessage toDomain(NotificationMessageEntity entity) {
        return new NotificationMessage(
                new NotificationMessageId(entity.getMessageId()),
                new NotificationRequestId(entity.getRequestId()),
                NotificationType.valueOf(entity.getNotificationType()),
                toRecipient(entity),
                new NotificationContent(
                        entity.getTitle(),
                        entity.getBody(),
                        entity.getRedirectUrl(),
                        entity.getImageUrl()),
                toSenderInfo(entity),
                DeliveryStatus.valueOf(entity.getDeliveryStatus()),
                InstantDateTimeBridge.toInstant(entity.getScheduledAt()),
                InstantDateTimeBridge.toInstant(entity.getDispatchedAt()),
                entity.getFailureReason(),
                InstantDateTimeBridge.toInstant(entity.getCreatedAt()));
    }

    //
    private Recipient toRecipient(NotificationMessageEntity entity) {
        return new Recipient(
                entity.getUserId(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getDeviceToken(),
                entity.getLanguage());
    }

    //
    private SenderInfo toSenderInfo(NotificationMessageEntity entity) {
        switch (NotificationType.valueOf(entity.getNotificationType())) {
            case EMAIL:
                return new EmailSender(entity.getSenderId(),
                        entity.getSenderEmailAddress(),
                        entity.getSenderName());
            case SMS:
                return new SmsSender(entity.getSenderId(),
                        entity.getSenderPhoneNumber(),
                        entity.getSenderName());
            case PUSH:
                return new PushSender(entity.getSenderId(),
                        entity.getSenderName());
            default:
                return null; // 지원하지 않는 타입
        }
    }

}
