package notification.domain.vo;

import notification.definition.annotations.ValueObject;

@ValueObject
public record NotificationContent(
        String title,
        String body,
        String redirectUrl,
        String imageUrl) {
}