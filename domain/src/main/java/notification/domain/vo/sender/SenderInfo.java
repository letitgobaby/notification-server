package notification.domain.vo.sender;

public sealed interface SenderInfo permits SmsSender, EmailSender, PushSender {
}