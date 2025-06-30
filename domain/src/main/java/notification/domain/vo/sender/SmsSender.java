package notification.domain.vo.sender;

import notification.definition.annotations.ValueObject;

@ValueObject
public record SmsSender(String senderPhoneNumber, String senderName) implements SenderInfo {
}
