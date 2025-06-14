package notification.domain.notification.vo;

import java.util.Collections;
import java.util.Map;

import notification.domain.common.annotations.ValueObject;
import notification.domain.common.exceptions.DomainValidationException;

@ValueObject
public record PushContent(
        String templateId, // 푸시 템플릿 ID (선택 사항)
        Map<String, Object> templateVariables, // 템플릿 변수 (선택 사항)
        String title, // 제목 (템플릿 미사용 시 직접 지정)
        String body, // 본문 (템플릿 미사용 시 직접 지정)
        String imageUrl, // 이미지 URL (optional)
        String deepLink, // 딥링크 (optional)
        Map<String, String> customData // 추가 데이터 (key-value)
) implements NotificationContent {

    public PushContent {
        // 템플릿 사용 시: templateId가 있어야 하고, title/body는 템플릿이 대체
        // 템플릿 미사용 시: title과 body가 필수
        boolean hasTemplate = templateId != null && !templateId.isBlank();
        boolean hasDirectTitle = title != null && !title.isBlank();
        boolean hasDirectBody = body != null && !body.isBlank();

        if (!hasTemplate && (!hasDirectTitle || !hasDirectBody)) {
            throw new DomainValidationException(
                    "Push content must have either a template ID or both a title and body.");
        }
        if (hasTemplate && templateVariables == null) {
            throw new DomainValidationException("Template variables cannot be null if a template ID is provided.");
        }

        templateVariables = templateVariables == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(templateVariables);
        customData = customData == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(customData);
    }

    @Override
    public String toReadableString() {
        if (templateId != null && !templateId.isBlank()) {
            return "Push (Template: " + templateId + ")";
        }
        return "Push (Direct): " + title + " - " + body;
    }

}