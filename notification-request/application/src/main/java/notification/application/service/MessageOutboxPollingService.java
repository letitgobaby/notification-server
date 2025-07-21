package notification.application.service;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.outbox.port.inbound.MessageOutboxPollingUseCase;
import notification.application.outbox.port.outbound.MessageOutboxEventPublisherPort;
import notification.application.outbox.port.outbound.MessageOutboxRepositoryPort;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageOutboxPollingService implements MessageOutboxPollingUseCase {

    private final MessageOutboxRepositoryPort messageOutboxRepository;
    private final MessageOutboxEventPublisherPort MessageOutboxEventPublisher;

    /**
     * Outbox 메시지를 발행합니다. Pending 및 Failed 상태의 메시지를 조회하여
     * MessageOutbox Event를 발행합니다.
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> poll() {
        return messageOutboxRepository.findPendingAndFailedMessages()
                .flatMap(outbox -> MessageOutboxEventPublisher.publish(outbox))
                .doOnError(e -> log.error("Error processing outbox message", e))
                .then();
    }

}
