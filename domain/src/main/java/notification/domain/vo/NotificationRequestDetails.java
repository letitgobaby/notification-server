package notification.domain.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.PolicyViolationException;

@ValueObject
public record NotificationRequestDetails(
        boolean useTemplate,
        TemplateInfo templateInfo,
        NotificationContent directContent,
        List<NotificationChannelConfig> channelConfigs // 채널 설정 정보
) {

    public NotificationRequestDetails {
        if (useTemplate && templateInfo == null)
            throw new PolicyViolationException("TemplateInfo must be provided when useTemplate is true.");
        if (!useTemplate && directContent == null)
            throw new PolicyViolationException("Direct content must be provided when useTemplate is false.");
        if (useTemplate && directContent != null)
            throw new PolicyViolationException("Cannot provide direct content when using a template.");
        if (!useTemplate && templateInfo != null)
            throw new PolicyViolationException("Cannot provide template info when not using a template."); // 중복 방지

        Objects.requireNonNull(channelConfigs, "Channel configurations cannot be null");
        if (channelConfigs.isEmpty())
            throw new IllegalArgumentException("At least one channel configuration must be provided.");

        channelConfigs = Collections.unmodifiableList(new ArrayList<>(channelConfigs));
    }

    /**
     * 템플릿 ID와 파라미터, 채널 설정을 사용하여 알림 요청 세부 정보를 생성합니다.
     * 
     * @param templateId
     * @param parameters
     * @param channelConfigs
     * @return
     */
    public static NotificationRequestDetails forTemplate(
            TemplateId templateId, Map<String, String> parameters,
            List<NotificationChannelConfig> channelConfigs) {

        if (templateId == null || channelConfigs == null || channelConfigs.isEmpty()) {
            throw new PolicyViolationException(
                    "Template ID and channel configurations must be provided for template requests.");
        }

        return new NotificationRequestDetails(true,
                new TemplateInfo(templateId, parameters),
                null,
                channelConfigs);
    }

    /**
     * 제목, 본문, 리디렉션 URL, 이미지 URL, 채널 설정을 사용하여 직접 콘텐츠 알림 요청 세부 정보를 생성합니다.
     * 
     * @param title
     * @param body
     * @param redirectUrl
     * @param imageUrl
     * @param channelConfigs
     * @return
     */
    public static NotificationRequestDetails forDirectContent(
            String title, String body, String redirectUrl, String imageUrl,
            List<NotificationChannelConfig> channelConfigs) {

        if (title == null || body == null || channelConfigs == null || channelConfigs.isEmpty()) {
            throw new PolicyViolationException(
                    "Title, body, and channel configurations must be provided for direct content requests.");
        }

        return new NotificationRequestDetails(false,
                null,
                new NotificationContent(title, body, redirectUrl, imageUrl),
                channelConfigs);
    }
}