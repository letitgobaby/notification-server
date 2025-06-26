package notification.application.service.support;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.outbound.persistence.NotificationRequestRepositoryPort;
import notification.application.notifiation.port.outbound.persistence.RequestOutboxMessageRepositoryPort;
import notification.definition.enums.RequestStatus;
import notification.domain.NotificationRequest;
import notification.domain.RequestOutboxMessage;
import notification.domain.vo.NotificationRequestId;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestOutboxLoader {

    private final NotificationRequestRepositoryPort notificationRequestRepository;
    private final RequestOutboxMessageRepositoryPort requestOutboxMessageRepository;

    /**
     * RequestOutboxMessage를 로드하고, 해당 알림 요청이 존재하지 않거나 상태가 CANCELED인 경우
     * Outbox 메시지를 삭제합니다.
     *
     * @param message RequestOutboxMessage
     * @return NotificationRequest Mono
     */
    public Mono<NotificationRequest> load(RequestOutboxMessage message) {
        NotificationRequestId requestId = NotificationRequestId.of(message.getAggregateId());

        return notificationRequestRepository.findById(requestId)
                .switchIfEmpty(clearRequestOutboxMessage(message))
                .flatMap(domain -> {
                    // 상태가 CANCELED인 경우 예외를 발생시킵니다.
                    if (domain.getStatus() == RequestStatus.CANCELED) {
                        return clearRequestOutboxMessage(message);
                    }

                    return Mono.just(domain);
                });
    }

    /**
     * RequestOutboxMessage를 삭제합니다.
     *
     * @param message RequestOutboxMessage
     * @return Mono<Void>
     */
    private Mono<NotificationRequest> clearRequestOutboxMessage(RequestOutboxMessage message) {
        return requestOutboxMessageRepository.deleteById(message.getOutboxId())
                .then(Mono.empty());
    }

}
