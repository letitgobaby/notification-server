package notification.application.service.infrastructure.loader;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.outbound.persistence.NotificationRequestRepositoryPort;
import notification.application.outbox.port.outbound.RequestOutboxRepositoryPort;
import notification.definition.vo.outbox.RequestOutbox;
import notification.domain.NotificationRequest;
import notification.domain.enums.RequestStatus;
import notification.domain.vo.NotificationRequestId;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestOutboxLoader {

    private final NotificationRequestRepositoryPort notificationRequestRepository;
    private final RequestOutboxRepositoryPort requestOutboxRepository;

    /**
     * RequestOutbox를 로드하고, 해당 알림 요청이 존재하지 않거나 상태가 CANCELED인 경우
     * Outbox 메시지를 삭제합니다.
     *
     * @param message RequestMessageOutbox
     * @return NotificationRequest Mono
     */
    public Mono<NotificationRequest> load(RequestOutbox message) {
        log.info("Loading NotificationRequest for outbox: {} / {}", message.getOutboxId(), message.getAggregateId());

        NotificationRequestId requestId = NotificationRequestId.of(message.getAggregateId());
        return notificationRequestRepository.findById(requestId)
                .switchIfEmpty(Mono.defer(() -> clearRequestMessageOutbox(message)))
                .flatMap(domain -> {
                    // 상태가 CANCELED인 경우 예외를 발생시킵니다.
                    if (domain.getStatus() == RequestStatus.CANCELED) {
                        return clearRequestMessageOutbox(message);
                    }

                    return Mono.just(domain);
                })
                .doOnError(error -> {
                    log.error("Error loading NotificationRequest for outbox: {}: {}", message.getOutboxId(),
                            error.getMessage(), error);
                });
    }

    /**
     * RequestMessageOutbox를 삭제합니다.
     *
     * @param message RequestMessageOutbox
     * @return Mono<Void>
     */
    private Mono<NotificationRequest> clearRequestMessageOutbox(RequestOutbox message) {
        return requestOutboxRepository.deleteById(message.getOutboxId())
                .then(Mono.empty());
    }

}
