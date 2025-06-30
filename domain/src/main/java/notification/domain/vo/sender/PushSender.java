package notification.domain.vo.sender;

import notification.definition.annotations.ValueObject;

@ValueObject
public record PushSender(String senderName) implements SenderInfo {
}