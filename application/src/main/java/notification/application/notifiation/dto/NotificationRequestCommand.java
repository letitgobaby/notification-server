package notification.application.notifiation.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import notification.definition.enums.NotificationType;
import notification.domain.vo.NotificationChannelConfig;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.Requester;
import notification.domain.vo.SenderInfo;
import notification.domain.vo.TargetAudience;
import notification.domain.vo.TemplateId;
import notification.domain.vo.TemplateInfo;

public record NotificationRequestCommand(
        String idempotencyKey, // 멱등성 키
        Requester requester, // 도메인 값 객체 직접 사용
        TargetAudience targetAudience, // 도메인 값 객체 직접 사용
        boolean useTemplate,
        TemplateInfoCommand templateInfo, // 커맨드 내부의 TemplateInfoCommand
        NotificationContentCommand directContent, // 커맨드 내부의 NotificationContentCommand
        List<NotificationChannelConfigCommand> channelConfigs, // 커맨드 내부의 ChannelConfigCommand
        Instant scheduledAt //
) {

    // 커맨드 객체 생성 시 기본적인 유효성 검사 (생성자에서 수행)
    public NotificationRequestCommand {
        Objects.requireNonNull(requester, "Requester cannot be null.");
        Objects.requireNonNull(targetAudience, "Target audience cannot be null.");
        Objects.requireNonNull(channelConfigs, "Channel configurations cannot be null.");
        if (channelConfigs.isEmpty()) {
            throw new IllegalArgumentException("At least one channel configuration is required.");
        }

        if (useTemplate) {
            if (templateInfo == null)
                throw new IllegalArgumentException("Template info must be provided when useTemplate is true.");
            if (directContent != null)
                throw new IllegalArgumentException("Cannot provide direct content when using a template.");
        } else {
            if (directContent == null)
                throw new IllegalArgumentException("Direct content must be provided when useTemplate is false.");
            if (templateInfo != null)
                throw new IllegalArgumentException("Cannot provide template info when not using a template.");
        }
    }

    // TemplateInfo 도메인 값 객체와 유사하지만, 커맨드용으로 분리
    public record TemplateInfoCommand(
            String templateId,
            Map<String, String> templateParameters) {

        public TemplateInfo toDomain() {
            return new TemplateInfo(new TemplateId(templateId), templateParameters);
        }
    }

    // NotificationContent 도메인 값 객체와 유사하지만, 커맨드용으로 분리
    public record NotificationContentCommand(
            String title,
            String body,
            String redirectUrl,
            String imageUrl) {

        public NotificationContent toDomain() {
            return new NotificationContent(title, body, redirectUrl, imageUrl);
        }
    }

    // NotificationChannelConfig 도메인 값 객체와 유사하지만, 커맨드용으로 분리
    public record NotificationChannelConfigCommand(
            NotificationType notificationType, // Enum은 공유
            SenderInfoCommand senderInfo) {

        public NotificationChannelConfig toDomain() {
            return new NotificationChannelConfig(notificationType, senderInfo.toDomain());
        }
    }

    // SenderInfo 도메인 값 객체와 유사하지만, 커맨드용으로 분리
    public record SenderInfoCommand(
            String senderPhoneNumber,
            String senderEmailAddress,
            String senderName) {

        public SenderInfo toDomain() {
            return new SenderInfo(senderPhoneNumber, senderEmailAddress, senderName);
        }
    }
}
