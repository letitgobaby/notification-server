package notification.domain.vo.sender;

import lombok.Getter;
import lombok.experimental.Accessors;
import notification.definition.annotations.ValueObject;
import notification.domain.enums.NotificationType;

@Getter
@Accessors(fluent = true)
@ValueObject
public final class EmailSender implements SenderInfo {

    private final String senderId;
    private final String senderEmailAddress;
    private final String senderName;

    public EmailSender(String senderId, String senderEmailAddress, String senderName) {
        this.senderId = senderId;
        this.senderEmailAddress = senderEmailAddress;
        this.senderName = senderName;
    }

    public EmailSender(String senderEmailAddress, String senderName) {
        this.senderId = null; // ID is optional
        this.senderEmailAddress = senderEmailAddress;
        this.senderName = senderName;
    }

    @Override
    public String getId() {
        return senderId;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }
}