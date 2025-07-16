package notification.definition.vo;

import notification.definition.annotations.ValueObject;

@ValueObject
public record RenderedContent(
        String title,
        String body,
        String language,
        String templateId) {

}
