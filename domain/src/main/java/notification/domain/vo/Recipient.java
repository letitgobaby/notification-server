package notification.domain.vo;

import java.util.Objects;

import notification.definition.annotations.ValueObject;
import notification.definition.enums.NotificationType;
import notification.definition.vo.UserId;

@ValueObject
public record Recipient(
        String phoneNumber,
        String email,
        String deviceToken,
        UserId userId) {
    public Recipient {
        // 최소 하나의 연락처 정보는 있어야 함
        if (phoneNumber == null && email == null && deviceToken == null) {
            throw new IllegalArgumentException("At least one contact method must be provided for Recipient");
        }
        // userId는 회원인 경우에만 존재, 비회원은 null
    }

    public static Recipient ofSms(String phoneNumber, UserId userId) {
        Objects.requireNonNull(phoneNumber, "Phone number cannot be null for SMS recipient");
        return new Recipient(phoneNumber, null, null, userId);
    }

    public static Recipient ofEmail(String emailAddress, UserId userId) {
        Objects.requireNonNull(emailAddress, "Email address cannot be null for Email recipient");
        return new Recipient(null, emailAddress, null, userId);
    }

    public static Recipient ofPush(String deviceToken, UserId userId) {
        Objects.requireNonNull(deviceToken, "Device token cannot be null for Push recipient");
        return new Recipient(null, null, deviceToken, userId);
    }

    // 비회원용 팩토리 메서드 (UserId 없이)
    public static Recipient ofSms(String phoneNumber) {
        return ofSms(phoneNumber, null);
    }

    public static Recipient ofEmail(String emailAddress) {
        return ofEmail(emailAddress, null);
    }

    public static Recipient ofPush(String deviceToken) {
        return ofPush(deviceToken, null);
    }

    public boolean supportType(NotificationType type) {
        return switch (type) {
            case SMS -> phoneNumber != null;
            case EMAIL -> email != null;
            case PUSH -> deviceToken != null;
        };
    }
}
