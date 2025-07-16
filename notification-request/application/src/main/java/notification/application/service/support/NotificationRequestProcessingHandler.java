package notification.application.service.support;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.outbound.persistence.NotificationRequestRepositoryPort;
import notification.application.outbox.port.outbound.MessageOutboxEventPublisherPort;
import notification.application.outbox.port.outbound.RequestOutboxRepositoryPort;
import notification.definition.vo.outbox.MessageOutbox;
import notification.domain.NotificationRequest;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestProcessingHandler {

    private final NotificationRequestRepositoryPort notificationRequestRepository;
    private final RequestOutboxRepositoryPort requestMessageOutboxRepository;
    private final NotificationRequestParser notificationMessageParser;
    private final NotificationMessageWithOutboxSaver notificationMessageWithOutboxSaver;
    private final MessageOutboxEventPublisherPort MessageOutboxEventPublisher;

    /**
     * NotificationRequest를 처리하고, 해당 요청에 대한 NotificationMessage를 생성하여 저장합니다.
     *
     * @param domain NotificationRequest
     * @return Mono<Void>
     */
    public Mono<List<MessageOutbox>> handle(NotificationRequest domain) {
        log.info("Handling NotificationRequest: {}", domain.getRequestId().value());

        String requestId = domain.getRequestId().value();

        // 알림 요청을 Processing 상태로 변경
        domain.markAsProcessing();

        return notificationRequestRepository.save(domain)
                .flatMapMany(notificationMessageParser::parse)
                .flatMap(notificationMessageWithOutboxSaver::save)
                .flatMap(this::publishMessageOutbox)
                .collectList()
                .doOnNext(savedList -> clearRequestMessageOutbox(requestId));
    }

    /**
     * Outbox 메시지를 삭제하여 요청에 대한 Outbox 메시지를 정리합니다.
     * 
     * @param requestId
     * @return
     */
    private Mono<Void> clearRequestMessageOutbox(String requestId) {
        log.info("Clearing request outbox messages for requestId: {}", requestId);

        return requestMessageOutboxRepository.deleteByAggregateId(requestId)
                .doOnError(err -> log.error("Failed to clear request outbox messages for requestId {}: {}",
                        requestId, err.getMessage(), err));
    }

    /**
     * Outbox 메시지를 발행합니다.
     *
     * @param MessageOutbox 발행할 Outbox 메시지
     * @return Mono<MessageOutbox>
     */
    private Mono<MessageOutbox> publishMessageOutbox(MessageOutbox MessageOutbox) {
        return MessageOutboxEventPublisher.publish(MessageOutbox)
                .thenReturn(MessageOutbox);
    }

}