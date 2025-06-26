package notification.domain;

class UserNotificationTest {

    // private UserNotificationHistoryId userNotificationId() {
    // // return UserNotificationHistoryId.generate();
    // return new UserNotificationHistoryId("user-notification-1");
    // }

    // private UserId userId() {
    // return new UserId("user-1");
    // }

    // private NotificationMessageId notificationItemId() {
    // return new NotificationMessageId("item-1");
    // }

    // private NotificationRequestId notificationRequestId() {
    // return new NotificationRequestId("req-1");
    // }

    // @Test
    // void createUserNotification_success() {
    // Instant sentAt = Instant.now().minusSeconds(10);
    // UserNotificationHistory notification = new UserNotificationHistory(
    // userNotificationId(),
    // userId(),
    // notificationItemId(),
    // notificationRequestId(),
    // NotificationType.EMAIL,
    // "Test Title",
    // "Test Content",
    // sentAt,
    // false,
    // "https://redirect.com",
    // "https://image.com/image.png",
    // DeliveryStatus.PENDING,
    // null);

    // assertNotNull(notification.getUserHistoryId());
    // assertEquals("user-1", notification.getUserId().value());
    // assertEquals("item-1", notification.getNotificationMessageId().value());
    // assertEquals("req-1", notification.getNotificationRequestId().value());
    // assertEquals(NotificationType.EMAIL, notification.getNotificationType());
    // assertEquals("Test Title", notification.getTitle());
    // assertEquals("Test Content", notification.getContent());
    // assertEquals(sentAt, notification.getSentAt());
    // assertFalse(notification.isRead());
    // assertEquals("https://redirect.com", notification.getRedirectUrl());
    // assertEquals("https://image.com/image.png", notification.getImageUrl());
    // assertEquals(DeliveryStatus.PENDING, notification.getDeliveryStatus());
    // assertNull(notification.getDeliveryFailureReason());
    // }

    // @Test
    // void createUserNotification_factoryMethod_success() {
    // Instant sentAt = Instant.now().minusSeconds(5);
    // UserNotificationHistory notification = UserNotificationHistory.create(
    // userId(),
    // notificationItemId(),
    // notificationRequestId(),
    // NotificationType.PUSH,
    // "Push Title",
    // "Push Content",
    // sentAt,
    // "https://redirect.com",
    // "https://image.com/img.png");

    // assertNotNull(notification.getUserHistoryId());
    // assertEquals(NotificationType.PUSH, notification.getNotificationType());
    // assertEquals("Push Title", notification.getTitle());
    // assertEquals("Push Content", notification.getContent());
    // assertEquals(sentAt, notification.getSentAt());
    // assertFalse(notification.isRead());
    // assertEquals("https://redirect.com", notification.getRedirectUrl());
    // assertEquals("https://image.com/img.png", notification.getImageUrl());
    // assertEquals(DeliveryStatus.PENDING, notification.getDeliveryStatus());
    // assertNull(notification.getDeliveryFailureReason());
    // }

    // @Test
    // void constructor_nullMandatoryField_throwsMandatoryFieldNullException() {
    // Instant sentAt = Instant.now().minusSeconds(10);

    // assertThrows(MandatoryFieldNullException.class, () -> new
    // UserNotificationHistory(
    // null,
    // userId(),
    // notificationItemId(),
    // notificationRequestId(),
    // NotificationType.EMAIL,
    // "title",
    // "content",
    // sentAt,
    // false,
    // null,
    // null,
    // DeliveryStatus.PENDING,
    // null));
    // }

    // @Test
    // void constructor_sentAtInFuture_throwsIllegalArgumentException() {
    // Instant future = Instant.now().plusSeconds(60);

    // assertThrows(IllegalArgumentException.class, () -> new
    // UserNotificationHistory(
    // userNotificationId(),
    // userId(),
    // notificationItemId(),
    // notificationRequestId(),
    // NotificationType.SMS,
    // "title",
    // "content",
    // future,
    // false,
    // null,
    // null,
    // DeliveryStatus.PENDING,
    // null));
    // }

    // @Test
    // void constructor_nullDeliveryStatus_throwsMandatoryFieldNullException() {
    // Instant sentAt = Instant.now().minusSeconds(10);

    // assertThrows(MandatoryFieldNullException.class, () -> new
    // UserNotificationHistory(
    // userNotificationId(),
    // userId(),
    // notificationItemId(),
    // notificationRequestId(),
    // NotificationType.EMAIL,
    // "title",
    // "content",
    // sentAt,
    // false,
    // null,
    // null,
    // null,
    // null));
    // }
}