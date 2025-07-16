package notification.definition.vo;

import notification.definition.annotations.ValueObject;

/**
 * 템플릿 렌더링을 위한 키 객체
 */
@ValueObject
public record TemplateRenderKey(String templateId, String type, String language) {
    @Override
    public String toString() {
        return String.format("%s_%s_%s", templateId, type, language);
    }
}
