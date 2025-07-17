package notification.domain.vo;

import java.io.Serializable;

import lombok.Getter;

@Getter
public class NotificationContent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String contentId;
    private final String title;
    private final String body;
    private final String redirectUrl;
    private final String imageUrl;

    public NotificationContent(String contentId, String title, String body, String redirectUrl, String imageUrl) {
        this.contentId = contentId;
        this.title = title;
        this.body = body;
        this.redirectUrl = redirectUrl;
        this.imageUrl = imageUrl;

    }

    public NotificationContent(String title, String body, String redirectUrl, String imageUrl) {
        this.contentId = null; // contentId will be set by the entity
        this.title = title;
        this.body = body;
        this.redirectUrl = redirectUrl;
        this.imageUrl = imageUrl;
    }

}