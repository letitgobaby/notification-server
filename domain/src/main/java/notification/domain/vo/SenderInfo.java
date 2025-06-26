package notification.domain.vo;

import java.util.Objects;

import notification.definition.annotations.ValueObject;

@ValueObject
public record SenderInfo(String senderPhoneNumber, String senderEmailAddress, String senderName) {
    public static SenderInfo ofSmsSender(String phoneNumber) {
        Objects.requireNonNull(phoneNumber);
        return new SenderInfo(phoneNumber, null, null);
    }

    public static SenderInfo ofEmailSender(String emailAddress, String senderName) {
        Objects.requireNonNull(emailAddress);
        return new SenderInfo(null, emailAddress, senderName);
    }

    public static SenderInfo ofPushSender(String senderName) {
        Objects.requireNonNull(senderName);
        return new SenderInfo(null, null, senderName);
    }
}