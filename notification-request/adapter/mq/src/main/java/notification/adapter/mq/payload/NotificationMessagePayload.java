package notification.adapter.mq.payload;

public sealed interface NotificationMessagePayload
        permits EmailMessagePayload, SmsMessagePayload, PushMessagePayload {

    String getMessageId();

    String getNotificationType(); // "EMAIL", "SMS", "PUSH"
}
