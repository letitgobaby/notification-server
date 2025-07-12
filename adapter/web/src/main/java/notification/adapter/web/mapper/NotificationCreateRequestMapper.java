package notification.adapter.web.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import notification.adapter.web.dto.request.NotificationCreateRequest;
import notification.adapter.web.dto.request.NotificationCreateRequest.ContentRequest;
import notification.adapter.web.dto.request.NotificationCreateRequest.DirectRecipientRequest;
import notification.adapter.web.dto.request.NotificationCreateRequest.RecipientsRequest;
import notification.adapter.web.dto.request.NotificationCreateRequest.RequesterRequest;
import notification.adapter.web.dto.request.NotificationCreateRequest.SenderInfoRequest;
import notification.adapter.web.dto.request.NotificationCreateRequest.TemplateRequest;
import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.ContentCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.DirectRecipientCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.RecipientsCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.RequesterCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.SenderInfoCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.TemplateCommand;
import notification.domain.enums.NotificationType;

@Component
public class NotificationCreateRequestMapper {

    /**
     * NotificationCreateRequest DTO를 NotificationRequestCommand 커맨드로 변환합니다.
     */
    public NotificationRequestCommand toCommand(NotificationCreateRequest request) {
        if (request == null) {
            return null;
        }

        RequesterCommand requesterCommand = toRequesterCommand(request.getRequester());
        RecipientsCommand recipientsCommand = toRecipientsCommand(request.getRecipients());
        ContentCommand contentCommand = toContentCommand(request.getContent());
        TemplateCommand templateCommand = toTemplateCommand(request.getTemplate());

        Map<NotificationType, SenderInfoCommand> senderInfosCommand = null;
        if (request.getSenderInfos() != null) {
            senderInfosCommand = request.getSenderInfos().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> toSenderInfoCommand(entry.getValue())));
        }

        return new NotificationRequestCommand(
                requesterCommand,
                recipientsCommand,
                request.getNotificationTypes(),
                senderInfosCommand,
                contentCommand,
                templateCommand,
                request.getScheduledAt(),
                request.getMemo());
    }

    private RequesterCommand toRequesterCommand(RequesterRequest request) {
        if (request == null) {
            return null;
        }
        return new RequesterCommand(request.getType(), request.getId());
    }

    private RecipientsCommand toRecipientsCommand(RecipientsRequest request) {
        if (request == null) {
            return null;
        }

        List<DirectRecipientCommand> directRecipientsCommand = null;
        if (request.getDirectRecipients() != null) {
            directRecipientsCommand = request.getDirectRecipients().stream()
                    .map(this::toDirectRecipientCommand)
                    .collect(Collectors.toList());
        }

        return new RecipientsCommand(
                request.getUserIds(),
                directRecipientsCommand,
                request.getSegment(),
                request.getAllUsers());
    }

    private DirectRecipientCommand toDirectRecipientCommand(DirectRecipientRequest request) {
        if (request == null) {
            return null;
        }

        return new DirectRecipientCommand(request.getPhoneNumber(), request.getEmail(), request.getDeviceToken());
    }

    private SenderInfoCommand toSenderInfoCommand(SenderInfoRequest request) {
        if (request == null) {
            return null;
        }

        return new SenderInfoCommand(request.getSenderPhoneNumber(), request.getSenderEmailAddress(),
                request.getSenderName());
    }

    private ContentCommand toContentCommand(ContentRequest request) {
        if (request == null) {
            return null;
        }

        return new ContentCommand(request.getTitle(), request.getBody(),
                request.getRedirectUrl(), request.getImageUrl());
    }

    private TemplateCommand toTemplateCommand(TemplateRequest request) {
        if (request == null) {
            return null;
        }

        return new TemplateCommand(request.getTemplateId(), request.getTemplateParameters());
    }
}
