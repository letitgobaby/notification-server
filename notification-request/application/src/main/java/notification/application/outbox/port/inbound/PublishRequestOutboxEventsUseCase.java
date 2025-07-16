package notification.application.outbox.port.inbound;

import reactor.core.publisher.Mono;

public interface PublishRequestOutboxEventsUseCase {

    Mono<Void> publish();

}
