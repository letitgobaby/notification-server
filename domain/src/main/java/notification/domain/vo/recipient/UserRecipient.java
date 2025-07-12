package notification.domain.vo.recipient;

import lombok.Getter;
import lombok.experimental.Accessors;
import notification.definition.annotations.ValueObject;
import notification.domain.enums.RecipientType;
import notification.domain.vo.UserId;

@Getter
@Accessors(fluent = true)
@ValueObject
public final class UserRecipient implements RecipientReference {

    private final String recipientId;
    private final UserId userId;

    public UserRecipient(String recipientId, UserId userId) {
        this.recipientId = recipientId;
        this.userId = userId;
    }

    public UserRecipient(UserId userId) {
        this.recipientId = null;
        this.userId = userId;
    }

    @Override
    public String getId() {
        return recipientId;
    }

    @Override
    public RecipientType getType() {
        return RecipientType.USER;
    }
}