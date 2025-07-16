package notification.application.notifiation.port.outbound.persistence;

import notification.domain.NotificationMessage;
import notification.domain.vo.NotificationMessageId;
import reactor.core.publisher.Mono;

public interface NotificationMessageRepositoryPort {

    /**
     * Saves the notification message to the repository.
     *
     * @param notificationMessage the notification message to save
     * @return the saved notification message
     */
    Mono<NotificationMessage> save(NotificationMessage domain);

    /**
     * Updates the notification message in the repository.
     *
     * @param domain the notification message to update
     * @return the updated notification message
     */
    Mono<NotificationMessage> update(NotificationMessage domain);

    /**
     * Finds a notification message by its ID.
     *
     * @param id the ID of the notification message
     * @return the found notification message, or null if not found
     */
    Mono<NotificationMessage> findById(NotificationMessageId id);

    /**
     * Deletes a notification message by its ID.
     *
     * @param id the ID of the notification message to delete
     */
    Mono<Void> deleteById(NotificationMessageId id);

}
