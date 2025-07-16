package notification.domain.vo.sender;

import notification.domain.enums.NotificationType;

public sealed interface SenderInfo permits SmsSender, EmailSender, PushSender {

    String getId();

    NotificationType getType();
}