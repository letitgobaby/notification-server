package notification.application.service.event.publisher;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.outbox.port.inbound.PublishRequestOutboxEventsUseCase;
import notification.application.outbox.port.outbound.RequestOutboxEventPublisherPort;
import notification.application.outbox.port.outbound.RequestOutboxRepositoryPort;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishRequestOutboxEventsService implements PublishRequestOutboxEventsUseCase {

    private final RequestOutboxRepositoryPort requestOutboxRepository;
    private final RequestOutboxEventPublisherPort requestOutboxEventPublisher;

    /**
     * Outbox 메시지를 발행합니다. Pending 및 Failed 상태의 메시지를 조회하여
     * RequestOutbox Event를 발행합니다.
     * 
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> publish() {
        return requestOutboxRepository.findPendingAndFailedMessages()
                .flatMap(outbox -> requestOutboxEventPublisher.publish(outbox))
                .doOnError(e -> log.error("Error processing outbox message", e))
                .then();
    }

}
