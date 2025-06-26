package notification.application.notifiation.port.outbound.persistence;

import notification.domain.UserNotificationHistory;
import reactor.core.publisher.Mono;

public interface UserNotificationHistoryRepositoryPort {
    /**
     * Saves the user notification to the repository.
     *
     * @param userNotification the user notification to save
     * @return the saved user notification
     */
    Mono<UserNotificationHistory> save(UserNotificationHistory domain);

    /**
     * Finds a user notification by its ID.
     *
     * @param id the ID of the user notification
     * @return the found user notification, or null if not found
     */
    Mono<UserNotificationHistory> findById(String id);

    /**
     * Deletes a user notification by its ID.
     *
     * @param id the ID of the user notification to delete
     */
    Mono<Void> deleteById(String id);

}