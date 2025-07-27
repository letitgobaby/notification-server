package notification.application.notifiation.port.inbound;

import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestResult;
import reactor.core.publisher.Mono;

public interface ProcessNotificationRequestUseCase {

    Mono<NotificationRequestResult> handle(
            NotificationRequestCommand command, String idempotencyKey);

}
