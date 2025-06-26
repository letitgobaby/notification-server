package notification.domain.vo;

import java.util.Objects;

import notification.definition.annotations.ValueObject;

@ValueObject
public record RenderedContent(String title, String content, String redirectUrl, String imageUrl) {
    public RenderedContent {
        Objects.requireNonNull(content, "Rendered content body cannot be null");
    }
}
