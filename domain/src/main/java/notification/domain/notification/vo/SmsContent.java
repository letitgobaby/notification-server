package notification.domain.notification.vo;

import java.util.Collections;
import java.util.Map;

import notification.domain.common.annotations.ValueObject;

@ValueObject
public record SmsContent(
        String templateId, // SMS 템플릿 ID (선택 사항)
        Map<String, Object> templateVariables, // 템플릿 변수 (선택 사항)
        String directMessage, // 템플릿 미사용 시 직접 메시지 (선택 사항)
        boolean isMarketing // 광고성 메시지 여부
) implements NotificationContent {

    public SmsContent {

        // 템플릿 ID와 직접 메시지 중 최소 하나는 존재해야 합니다.
        boolean hasTemplate = templateId != null && !templateId.isBlank();
        boolean hasDirectMessage = directMessage != null && !directMessage.isBlank();

        if (!hasTemplate && !hasDirectMessage) {
            throw new IllegalArgumentException("SMS content must have either a template ID or a direct message.");
        }

        // 템플릿 ID가 제공되면 templateVariables는 null이 아니어야 합니다.
        if (hasTemplate && templateVariables == null) {
            throw new IllegalArgumentException("Template variables cannot be null if a template ID is provided.");
        }

        // 템플릿 변수 맵은 불변 컬렉션으로 캡슐화
        templateVariables = templateVariables != null ? Collections.unmodifiableMap(templateVariables)
                : Collections.emptyMap();
    }

    @Override
    public String toReadableString() {
        if (templateId != null && !templateId.isBlank()) {
            return "SMS (Template: " + templateId + ")";
        }
        return "SMS (Direct Message): " + directMessage;
    }

}
