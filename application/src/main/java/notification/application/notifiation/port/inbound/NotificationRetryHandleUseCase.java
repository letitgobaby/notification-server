package notification.application.notifiation.port.inbound;

import reactor.core.publisher.Mono;

public interface NotificationRetryHandleUseCase {

    Mono<Void> pollRetryOutbox();

}
