package notification.domain.vo;

import java.util.Map;

import notification.definition.annotations.ValueObject;

@ValueObject
public record TemplateInfo(
        String templateId,
        Map<String, String> parameters) {
}