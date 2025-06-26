package notification.application.template.dto;

public record TemplateDefinition(
        String templateId,
        String language,
        String titleTemplate,
        String bodyTemplate) {

}
