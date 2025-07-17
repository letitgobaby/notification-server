package notification.application.service.support;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import notification.definition.vo.CollectedData;
import notification.definition.vo.RenderedContent;
import notification.definition.vo.TemplateRenderKey;
import notification.definition.vo.UserConfig;
import notification.domain.NotificationRequest;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.recipient.Recipient;

/**
 * 템플릿과 파라미터를 조합하여 최종 알림 콘텐츠를 생성하는 서비스
 * 
 * 렌더링된 템플릿에 사용자별 파라미터를 적용하여
 * 실제 사용자에게 전송될 최종 콘텐츠를 생성합니다.
 */
@Slf4j
@Component
public class NotificationContentBuilder {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final String DEFAULT_LANGUAGE = "ko";

    /**
     * 알림 콘텐츠를 생성합니다.
     * 템플릿이 있으면 템플릿 기반으로, 없으면 직접 콘텐츠를 반환합니다.
     * 
     * @param request
     * @param recipient
     * @param type
     * @param data
     * @return
     */
    public NotificationContent createContent(NotificationRequest request, Recipient recipient,
            NotificationType type, CollectedData<UserConfig> data) {
        if (request.getTemplate() == null) {
            return request.getContent();
        }

        String templateKey = new TemplateRenderKey(
                request.getTemplate().getTemplateId(),
                type.name(),
                recipient.language() != null ? recipient.language() : DEFAULT_LANGUAGE)
                .toString();

        RenderedContent rendered = data.renderedTemplates().get(templateKey);
        if (rendered == null) {
            log.warn("Rendered template not found for key: {}, using fallback content", templateKey);
            return request.getContent();
        }

        // 파라미터 적용
        Map<String, String> parameters = request.getTemplate().getParameters();
        String finalTitle = applyParameters(rendered.title(), parameters, recipient, data);
        String finalBody = applyParameters(rendered.body(), parameters, recipient, data);

        return new NotificationContent(finalTitle, finalBody, null, null);
    }

    /**
     * 렌더링된 콘텐츠에 파라미터를 적용합니다.
     */
    private String applyParameters(String content, Map<String, String> parameters, Recipient recipient,
            CollectedData<UserConfig> data) {
        if (content == null || parameters == null) {
            return content;
        }

        String result = content;
        result = applyUserConfigParameters(result, recipient, data);
        result = applyTemplateParameters(result, parameters);

        return result;
    }

    /**
     * 수신자 정보와 UserConfig 정보를 템플릿에 적용합니다.
     */
    private String applyUserConfigParameters(String content, Recipient recipient, CollectedData<UserConfig> data) {
        String result = content;

        // UserConfig 정보 적용 (userId가 있는 경우에만)
        if (recipient.userId() != null) {
            UserConfig userConfig = data.userConfigs().get(recipient.userId());
            if (userConfig != null) {
                // UserConfig의 추가 정보들을 플레이스홀더로 적용
                if (userConfig.userName() != null) {
                    result = result.replace("{{userName}}", userConfig.userName());
                }
            }
        }

        return result;
    }

    /**
     * 템플릿 파라미터를 적용합니다.
     */
    private String applyTemplateParameters(String content, Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return content;
        }

        String result = content;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        while (matcher.find()) {
            String placeholder = matcher.group(0); // {{key}}
            String key = matcher.group(1); // key

            String value = parameters.get(key);
            if (value != null) {
                result = result.replace(placeholder, value);
            } else {
                log.debug("Parameter not found for placeholder: {}", placeholder);
            }
        }

        return result;
    }
}
