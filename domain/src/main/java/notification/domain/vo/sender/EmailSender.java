package notification.domain.vo.sender;

import notification.definition.annotations.ValueObject;

@ValueObject
public record EmailSender(
        String senderEmailAddress,
        String senderName) implements SenderInfo {
}