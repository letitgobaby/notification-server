package notification.application.outbox.port.outbound;

import notification.definition.vo.outbox.RequestOutbox;
import reactor.core.publisher.Mono;

public interface RequestOutboxEventPublisherPort {

    /**
     * Outbox 메시지를 발행합니다.
     *
     * @param RequestOutbox 발행할 Outbox 메시지
     * @return Mono<Void> 발행 완료를 나타내는 Mono
     */
    Mono<Void> publish(RequestOutbox requestOutbox);

}
