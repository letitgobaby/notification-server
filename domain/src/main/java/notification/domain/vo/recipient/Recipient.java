package notification.domain.vo.recipient;

import notification.definition.annotations.ValueObject;

@ValueObject
public record Recipient(
        String userId,
        String email,
        String phoneNumber,
        String deviceToken,
        String language) {
}