package notification.application.service.support;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.JsonPayloadFactory;
import notification.application.notifiation.port.outbound.persistence.NotificationMessageRepositoryPort;
import notification.application.notifiation.port.outbound.persistence.OutboxMessageRepositoryPort;
import notification.domain.NotificationMessage;
import notification.domain.OutboxMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageWithOutboxSaver {

    private final NotificationMessageRepositoryPort notificationMessageRepository;
    private final OutboxMessageRepositoryPort outboxMessageRepository;
    private final JsonPayloadFactory jsonPayloadFactory;

    /**
     * NotificationMessage를 저장하고, OutboxMessage를 생성하여 저장합니다.
     *
     * @param message NotificationMessage
     * @return OutboxMessage Mono
     */
    public Mono<OutboxMessage> save(NotificationMessage message) {
        return notificationMessageRepository.save(message)
                .flatMap(saved -> {
                    OutboxMessage outboxMessage = OutboxMessage.create(
                            saved.getClass().getSimpleName(),
                            saved.getItemId().value(),
                            "NotificationMessageScheduledEvent",
                            jsonPayloadFactory.toJsonPayload(saved),
                            saved.getScheduledAt());

                    return outboxMessageRepository.save(outboxMessage);
                });
    }

}
