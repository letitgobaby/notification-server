package notification.application.outbox.port.inbound;

import reactor.core.publisher.Mono;

public interface MessageOutboxPollingUseCase {

    Mono<Void> poll();

}
