package notification.definition.vo;

import notification.definition.annotations.ValueObject;

@ValueObject
public record TemplateDefinition(
        String templateId,
        String language,
        String titleTemplate,
        String bodyTemplate) {
}
