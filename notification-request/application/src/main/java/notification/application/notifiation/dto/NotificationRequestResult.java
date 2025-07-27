package notification.application.notifiation.dto;

public record NotificationRequestResult(
        String notificationId,
        String status,
        String message) {

    public static NotificationRequestResult success(String notificationId) {
        return new NotificationRequestResult(notificationId, "SUCCESS",
                "Notification request registered successfully.");
    }

    public static NotificationRequestResult failure() {
        return new NotificationRequestResult(null, "FAILURE",
                "Failed to register notification request.");
    }

    public static NotificationRequestResult failure(String message) {
        return new NotificationRequestResult(null, "FAILURE", message);
    }
}
