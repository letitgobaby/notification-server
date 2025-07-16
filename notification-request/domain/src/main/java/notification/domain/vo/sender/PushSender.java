package notification.domain.vo.sender;

import lombok.Getter;
import lombok.experimental.Accessors;
import notification.definition.annotations.ValueObject;
import notification.domain.enums.NotificationType;

@Getter
@Accessors(fluent = true)
@ValueObject
public final class PushSender implements SenderInfo {

    private final String senderId;
    private final String senderName;

    public PushSender(String senderId, String senderName) {
        this.senderId = senderId;
        this.senderName = senderName;
    }

    public PushSender(String senderName) {
        this.senderId = null; // ID is optional
        this.senderName = senderName;
    }

    @Override
    public String getId() {
        return senderId;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.PUSH;
    }
}