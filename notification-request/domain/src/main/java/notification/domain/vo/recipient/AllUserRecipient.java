package notification.domain.vo.recipient;

import lombok.Getter;
import lombok.experimental.Accessors;
import notification.definition.annotations.ValueObject;
import notification.domain.enums.RecipientType;

@Getter
@Accessors(fluent = true)
@ValueObject
public final class AllUserRecipient implements RecipientReference {

    private final String recipientId;

    public AllUserRecipient(String recipientId) {
        this.recipientId = recipientId;
    }

    public AllUserRecipient() {
        this.recipientId = null;
    }

    @Override
    public String getId() {
        return recipientId;
    }

    @Override
    public RecipientType getType() {
        return RecipientType.ALL_USER;
    }
}