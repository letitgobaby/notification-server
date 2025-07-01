package notification.application.notifiation.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.ContentCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.DirectRecipientCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.RecipientsCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.SenderInfoCommand;
import notification.domain.NotificationRequest;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.Requester;
import notification.domain.vo.TemplateInfo;
import notification.domain.vo.UserId;
import notification.domain.vo.recipient.AllUserRecipient;
import notification.domain.vo.recipient.DirectRecipient;
import notification.domain.vo.recipient.RecipientReference;
import notification.domain.vo.recipient.SegmentRecipient;
import notification.domain.vo.recipient.UserRecipient;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SenderInfo;
import notification.domain.vo.sender.SmsSender;
import reactor.core.publisher.Mono;

@Component
public class NotificationRequestMapper {

    /**
     * NotificationRequestCommand를 NotificationRequest 도메인 객체로 변환합니다.
     *
     * @param command 컨트롤러에서 전달받은 Command 객체
     * @return NotificationRequest 도메인 객체를 포함하는 Mono
     */
    public Mono<NotificationRequest> fromCommand(NotificationRequestCommand command) {
        if (command == null) {
            return Mono.error(new IllegalArgumentException("NotificationRequestCommand cannot be null"));
        }

        return Mono.fromCallable(() -> NotificationRequest.create(
                requesterFromCommand(command),
                recipientsFromCommand(command.recipients()),
                command.notificationTypes(),
                senderInfosFromCommand(command.senderInfos()),
                contentFromCommand(command.content()),
                templateFromCommand(command),
                command.memo(),
                command.scheduledAt()));
    }

    /**
     * 요청자 정보를 도메인 객체로 변환합니다.
     *
     * @param requesterCommand 요청자 정보 Command
     * @return 도메인 객체로 변환된 요청자 정보
     */
    private List<RecipientReference> recipientsFromCommand(RecipientsCommand recipientsCommand) {
        if (recipientsCommand == null) {
            return List.of();
        }

        if (recipientsCommand.allUsers()) {
            return List.of(new AllUserRecipient());
        }

        if (recipientsCommand.segment() != null && !recipientsCommand.segment().isEmpty()) {
            return List.of(new SegmentRecipient(recipientsCommand.segment()));
        }

        List<RecipientReference> recipients = new ArrayList<>();
        if (!recipientsCommand.userIds().isEmpty()) {
            recipients.addAll(recipientsCommand.userIds().stream()
                    .map(UserId::new)
                    .map(UserRecipient::new)
                    .toList());
        }

        if (!recipientsCommand.directRecipients().isEmpty()) {
            recipients.addAll(recipientsCommand.directRecipients().stream()
                    .map(this::mapDirectRecipient)
                    .toList());
        }

        return recipients;
    }

    /**
     * 발신자 정보를 Command에서 도메인 객체로 변환합니다.
     *
     * @param senderInfosCommand 발신자 정보 Command
     * @return 알림 채널별 발신자 정보 맵
     */
    private Map<NotificationType, SenderInfo> senderInfosFromCommand(
            Map<NotificationType, SenderInfoCommand> senderInfosCommand) {
        if (senderInfosCommand == null || senderInfosCommand.isEmpty()) {
            return Map.of();
        }

        Map<NotificationType, SenderInfo> senderInfos = new HashMap<>();
        senderInfosCommand.forEach((type, command) -> {
            if (command != null) {
                SenderInfo senderInfo = switch (type) {
                    case SMS -> new SmsSender(command.senderPhoneNumber(), command.senderName());
                    case EMAIL -> new EmailSender(command.senderEmailAddress(), command.senderName());
                    case PUSH -> new PushSender(command.senderName());
                    default -> throw new IllegalArgumentException("Unsupported notification type: " + type);
                };
                senderInfos.put(type, senderInfo);
            }
        });

        return senderInfos;
    }

    /**
     * 알림 내용을 Command에서 도메인 객체로 변환합니다.
     *
     * @param contentCommand 알림 내용 Command
     * @return NotificationContent 도메인 객체
     */
    private NotificationContent contentFromCommand(ContentCommand contentCommand) {
        if (contentCommand == null) {
            return null; // 또는 기본값을 반환할 수 있음
        }
        return new NotificationContent(
                contentCommand.title(),
                contentCommand.body(),
                contentCommand.redirectUrl(),
                contentCommand.imageUrl());
    }

    /**
     * NotificationRequestCommand에서 템플릿 정보를 도메인 객체로 변환합니다.
     *
     * @param command 알림 요청 Command
     * @return 템플릿 정보 도메인 객체
     */
    private TemplateInfo templateFromCommand(NotificationRequestCommand command) {
        return new TemplateInfo(command.template().templateId(), command.template().templateParameters());
    }

    /**
     * 요청자 정보를 Command에서 도메인 객체로 변환합니다.
     *
     * @param command 알림 요청 Command
     * @return Requester 도메인 객체
     */
    private Requester requesterFromCommand(NotificationRequestCommand command) {
        return new Requester(command.requester().type(), command.requester().id());
    }

    /**
     * DirectRecipientCommand를 DirectRecipient 도메인 객체로 변환합니다.
     *
     * @param command DirectRecipientCommand
     * @return DirectRecipient 도메인 객체
     */
    private DirectRecipient mapDirectRecipient(DirectRecipientCommand command) {
        if (command == null) {
            return null; // 또는 기본값을 반환할 수 있음
        }
        return new DirectRecipient(command.phoneNumber(), command.email(), command.deviceToken());
    }

}
