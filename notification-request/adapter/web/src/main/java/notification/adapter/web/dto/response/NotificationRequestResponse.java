package notification.adapter.web.dto.response;

public record NotificationRequestResponse(
        String notificationId,
        String status,
        String message) {
}
