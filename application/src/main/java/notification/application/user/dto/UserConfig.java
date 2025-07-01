package notification.application.user.dto;

import notification.domain.vo.recipient.Recipient;

public record UserConfig(
        String userId,
        String userName,
        String pushToken,
        String email,
        String phoneNumber,
        String language,
        String timeZone) {

    public Recipient toRecipient() {
        return new Recipient(userId, email, phoneNumber, pushToken, language);
    }

    // /**
    // * 사용자가 특정 알림 유형을 지원하는지 확인합니다.
    // *
    // * @param type
    // * @return
    // */
    // public boolean supportType(NotificationType type) {
    // return switch (type) {
    // case SMS -> phoneNumber != null && !phoneNumber.isBlank();
    // case EMAIL -> email != null && !email.isBlank();
    // case PUSH -> pushToken != null && !pushToken.isBlank();
    // default -> false;
    // };
    // }

    // /**
    // * 사용자 설정을 기반으로 알림 수신자를 생성합니다.
    // *
    // * @param type 알림 유형
    // * @return 알림 수신자
    // */
    // public Recipient toRecipient(NotificationType type) {

    // return switch (type) {
    // case SMS -> UserRecipient
    // case SMS -> SmsRecipient.of(phoneNumber, new UserId(userId));
    // case EMAIL -> EmailRecipient.of(email, new UserId(userId));
    // case PUSH -> PushRecipient.of(pushToken, new UserId(userId));
    // default -> throw new PolicyViolationException("Unsupported notification type:
    // " + type);
    // };
    // }
}
