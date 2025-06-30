package notification.domain.vo.recipient;

import notification.definition.annotations.ValueObject;

@ValueObject
public record DirectRecipient(
        String emailAddress,
        String phoneNumber,
        String deviceToken)
        implements RecipientReference {
}
