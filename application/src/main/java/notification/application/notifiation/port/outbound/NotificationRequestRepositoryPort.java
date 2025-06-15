package notification.application.notifiation.port.outbound;

import notification.domain.notification.NotificationRequest;
import notification.domain.notification.vo.NotificationId;
import reactor.core.publisher.Mono;

public interface NotificationRequestRepositoryPort {

    Mono<NotificationRequest> save(NotificationRequest request);

    Mono<NotificationRequest> findById(NotificationId notificationId);

}
