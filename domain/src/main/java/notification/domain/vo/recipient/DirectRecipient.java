package notification.domain.vo.recipient;

import lombok.Getter;
import lombok.experimental.Accessors;
import notification.definition.annotations.ValueObject;
import notification.domain.enums.RecipientType;

@Getter
@Accessors(fluent = true)
@ValueObject
public final class DirectRecipient implements RecipientReference {

    private final String recipientId;
    private final String emailAddress;
    private final String phoneNumber;
    private final String deviceToken;

    public DirectRecipient(String recipientId, String emailAddress, String phoneNumber, String deviceToken) {
        this.recipientId = recipientId;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.deviceToken = deviceToken;
    }

    public DirectRecipient(String emailAddress, String phoneNumber, String deviceToken) {
        this.recipientId = null;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.deviceToken = deviceToken;
    }

    @Override
    public String getId() {
        return recipientId;
    }

    @Override
    public RecipientType getType() {
        return RecipientType.DIRECT;
    }
}