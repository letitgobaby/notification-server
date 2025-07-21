package notification.application.service.processing.handler;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.outbox.port.outbound.MessageOutboxEventPublisherPort;
import notification.application.outbox.port.outbound.MessageOutboxRepositoryPort;
import notification.application.outbox.port.outbound.RequestOutboxRepositoryPort;
import notification.definition.annotations.UnitOfWork;
import notification.domain.NotificationRequest;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestOutboxHandler {

    private final RequestOutboxRepositoryPort requestOutboxRepository;
    private final MessageOutboxRepositoryPort messageOutboxRepository;
    private final MessageOutboxEventPublisherPort messageOutboxEventPublisher;

    /**
     * 알림 요청의 Outbox 메시지를 처리합니다. Outbox 메시지를 로드하고, 해당 메시지를 발행한 후
     * Outbox 메시지를 삭제합니다.
     *
     * @param domain NotificationRequest 알림 요청 도메인 객체
     * @return Mono<Void>
     */
    @UnitOfWork
    public Mono<Void> handle(NotificationRequest domain) {
        log.info("Handling NotificationRequest Outbox for: {}", domain.getRequestId().value());

        return Mono.just(domain.getRequestId().value())
                .flatMapMany(messageOutboxRepository::findByAggregateId)
                .flatMap(messageOutboxEventPublisher::publish)
                .doOnError(err -> log.error("Failed to publish outbox message: {}", err.getMessage(), err))
                .collectList()
                .flatMap(savedList -> requestOutboxRepository.deleteByAggregateId(domain.getRequestId().value()));
    }

}
