package notification.application.notifiation.factory;

import java.util.List;

import org.springframework.stereotype.Component;

import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.NotificationChannelConfigCommand;
import notification.domain.NotificationRequest;
import notification.domain.vo.NotificationChannelConfig;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationRequestDetails;
import notification.domain.vo.TemplateInfo;

@Component
public class NotificationRequestFactory {

    public NotificationRequest fromCommand(NotificationRequestCommand command) {
        List<NotificationChannelConfig> channelConfigs = command.channelConfigs().stream()
                .map(NotificationChannelConfigCommand::toDomain)
                .toList();

        NotificationRequestDetails requestDetails = command.useTemplate()
                ? createTemplateRequestDetails(command, channelConfigs)
                : createDirectContentRequestDetails(command, channelConfigs);

        return NotificationRequest.create(
                null,
                command.requester(),
                command.targetAudience(),
                requestDetails,
                command.scheduledAt());
    }

    private NotificationRequestDetails createTemplateRequestDetails(
            NotificationRequestCommand command,
            List<NotificationChannelConfig> channelConfigs) {
        TemplateInfo templateInfo = command.templateInfo().toDomain();
        return NotificationRequestDetails.forTemplate(
                templateInfo.templateId(),
                templateInfo.templateParameters(),
                channelConfigs);
    }

    private NotificationRequestDetails createDirectContentRequestDetails(
            NotificationRequestCommand command,
            List<NotificationChannelConfig> channelConfigs) {
        NotificationContent directContent = command.directContent().toDomain();
        return NotificationRequestDetails.forDirectContent(
                directContent.title(),
                directContent.body(),
                directContent.redirectUrl(),
                directContent.imageUrl(),
                channelConfigs);
    }

}
