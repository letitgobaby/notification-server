package notification.application.notifiation.port.outbound.persistence;

import notification.domain.NotificationRequest;
import notification.domain.vo.NotificationRequestId;
import reactor.core.publisher.Mono;

public interface NotificationRequestRepositoryPort {

    /**
     * 
     * @param request
     * @return NotificationRequest
     * @throws DuplicateNotificationRequestException.class
     */
    // Mono<NotificationRequest> save(NotificationRequest domain, String
    // idempotencyKey);
    Mono<NotificationRequest> save(NotificationRequest domain);

    /**
     * 
     * @param domain
     * @return NotificationRequest
     */
    Mono<NotificationRequest> update(NotificationRequest domain);

    /**
     * 
     * @param notificationId
     * @return NotificationRequest
     */
    Mono<NotificationRequest> findById(NotificationRequestId id);

    /**
     * 
     * @param id
     * @return Mono<Void>
     */
    Mono<Void> deleteById(NotificationRequestId id);

}
