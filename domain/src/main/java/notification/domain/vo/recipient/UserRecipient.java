package notification.domain.vo.recipient;

import notification.definition.annotations.ValueObject;
import notification.domain.vo.UserId;

@ValueObject
public record UserRecipient(UserId userId) implements RecipientReference {
}