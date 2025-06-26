package notification.application.service.support;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.outbound.persistence.NotificationMessageRepositoryPort;
import notification.application.notifiation.port.outbound.persistence.OutboxMessageRepositoryPort;
import notification.definition.enums.DeliveryStatus;
import notification.domain.NotificationMessage;
import notification.domain.OutboxMessage;
import notification.domain.vo.NotificationMessageId;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageOutboxLoader {

    private final NotificationMessageRepositoryPort notificationMessageRepository;
    private final OutboxMessageRepositoryPort outboxMessageRepository;

    /**
     * Outbox 메시지를 기반으로 NotificationMessage를 로드합니다.
     * 만약 해당 메시지가 존재하지 않거나 이미 DISPATCHED 상태인 경우 Outbox 메시지를 삭제합니다.
     *
     * @param outbox OutboxMessage
     * @return NotificationMessage Mono
     */
    public Mono<NotificationMessage> load(OutboxMessage outbox) {
        NotificationMessageId messageId = NotificationMessageId.of(outbox.getAggregateId());

        return notificationMessageRepository.findById(messageId)
                .switchIfEmpty(clearOutboxMessage(outbox))
                .flatMap(message -> {
                    // 이미 DISPATCHED 상태인 경우 Outbox 메시지를 삭제합니다.
                    if (message.getDeliveryStatus() == DeliveryStatus.DISPATCHED) {
                        return clearOutboxMessage(outbox);
                    }

                    return Mono.just(message);
                });
    }

    /**
     * Outbox 메시지를 삭제합니다.
     *
     * @param outbox OutboxMessage
     * @return Mono<Void>
     */
    private Mono<NotificationMessage> clearOutboxMessage(OutboxMessage outbox) {
        return outboxMessageRepository.deleteById(outbox.getOutboxId())
                .then(Mono.empty());
    }

}
