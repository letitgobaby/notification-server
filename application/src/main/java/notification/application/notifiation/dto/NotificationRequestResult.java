package notification.application.notifiation.dto;

import notification.domain.notification.vo.NotificationId;

public record NotificationRequestResult(
        NotificationId notificationId,
        String status,
        String message) {
}
