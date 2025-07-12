package notification.domain.vo.sender;

import lombok.Getter;
import lombok.experimental.Accessors;
import notification.definition.annotations.ValueObject;
import notification.domain.enums.NotificationType;

@Getter
@Accessors(fluent = true)
@ValueObject
public final class SmsSender implements SenderInfo {

    private final String senderId;
    private final String senderPhoneNumber;
    private final String senderName;

    public SmsSender(String senderId, String senderPhoneNumber, String senderName) {
        this.senderId = senderId;
        this.senderPhoneNumber = senderPhoneNumber;
        this.senderName = senderName;
    }

    public SmsSender(String senderPhoneNumber, String senderName) {
        this.senderId = null; // ID is optional
        this.senderPhoneNumber = senderPhoneNumber;
        this.senderName = senderName;
    }

    @Override
    public String getId() {
        return senderId;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.SMS;
    }
}