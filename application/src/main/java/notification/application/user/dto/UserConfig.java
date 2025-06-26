package notification.application.user.dto;

import notification.definition.enums.NotificationType;
import notification.definition.exceptions.PolicyViolationException;
import notification.definition.vo.UserId;
import notification.domain.vo.Recipient;

public record UserConfig(
        String userId,
        String userName,
        String pushToken,
        String email,
        String phoneNumber,
        String language,
        String timeZone) {

    /**
     * 사용자가 특정 알림 유형을 지원하는지 확인합니다.
     * 
     * @param type
     * @return
     */
    public boolean supportType(NotificationType type) {
        return switch (type) {
            case SMS -> phoneNumber != null && !phoneNumber.isBlank();
            case EMAIL -> email != null && !email.isBlank();
            case PUSH -> pushToken != null && !pushToken.isBlank();
            default -> false;
        };
    }

    /**
     * 사용자 설정을 기반으로 알림 수신자를 생성합니다.
     *
     * @param type 알림 유형
     * @return 알림 수신자
     */
    public Recipient toRecipient(NotificationType type) {
        return switch (type) {
            case SMS -> Recipient.ofSms(phoneNumber, new UserId(userId));
            case EMAIL -> Recipient.ofEmail(email, new UserId(userId));
            case PUSH -> Recipient.ofPush(pushToken, new UserId(userId));
            default -> throw new PolicyViolationException("Unsupported notification type: " + type);
        };
    }
}
