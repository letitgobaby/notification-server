package notification.definition.vo;

import notification.definition.annotations.ValueObject;

@ValueObject
public record UserConfig(
        String userId,
        String userName,
        String pushToken,
        String email,
        String phoneNumber,
        String language,
        String timeZone) {

}
