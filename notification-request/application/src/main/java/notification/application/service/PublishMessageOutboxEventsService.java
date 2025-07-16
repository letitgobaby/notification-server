package notification.application.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.outbox.port.inbound.PublishMessageOutboxEventsUseCase;
import notification.application.outbox.port.outbound.MessageOutboxEventPublisherPort;
import notification.application.outbox.port.outbound.MessageOutboxRepositoryPort;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishMessageOutboxEventsService implements PublishMessageOutboxEventsUseCase {

    private final MessageOutboxRepositoryPort MessageOutboxRepository;
    private final MessageOutboxEventPublisherPort MessageOutboxEventPublisher;

    /**
     * Outbox 메시지를 발행합니다. Pending 및 Failed 상태의 메시지를 조회하여
     * MessageOutbox Event를 발행합니다.
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> publish() {
        return MessageOutboxRepository.findPendingAndFailedMessages()
                .map(outbox -> {
                    MessageOutboxEventPublisher.publish(outbox);
                    return outbox;
                })
                .doOnError(e -> log.error("Error processing outbox message", e))
                .then();
    }

}
