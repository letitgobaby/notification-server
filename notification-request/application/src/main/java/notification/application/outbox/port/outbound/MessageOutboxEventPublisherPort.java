package notification.application.outbox.port.outbound;

import notification.definition.vo.outbox.MessageOutbox;
import reactor.core.publisher.Mono;

public interface MessageOutboxEventPublisherPort {

    /**
     * Outbox 메시지를 발행합니다.
     *
     * @param MessageOutbox 발행할 Outbox 메시지
     * 
     */
    Mono<Void> publish(MessageOutbox MessageOutbox);

}
