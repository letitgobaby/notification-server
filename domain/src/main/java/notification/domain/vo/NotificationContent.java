package notification.domain.vo;

import java.util.Objects;

import notification.definition.annotations.ValueObject;

@ValueObject
public record NotificationContent(
        String title,
        String body,
        String redirectUrl,
        String imageUrl) {

    public NotificationContent {
        Objects.requireNonNull(body, "Notification content body cannot be null");
    }

    public static NotificationContent fromTemplate(RenderedContent renderedContent) {
        Objects.requireNonNull(renderedContent, "Rendered content cannot be null");
        return new NotificationContent(
                renderedContent.title(),
                renderedContent.content(),
                renderedContent.redirectUrl(),
                renderedContent.imageUrl());
    }
}