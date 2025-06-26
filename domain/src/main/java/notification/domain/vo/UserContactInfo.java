package notification.domain.vo;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.MandatoryFieldException;
import notification.definition.vo.UserId;

@ValueObject
public record UserContactInfo(
        UserId userId,
        String phoneNumber,
        String emailAddress,
        String deviceToken) {

    public UserContactInfo {
        if (userId == null) {
            throw new MandatoryFieldException("User ID cannot be null");
        }
    }
}
