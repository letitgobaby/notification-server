package notification.application.service.infrastructure.saver;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.port.outbound.JsonPayloadConverterPort;
import notification.application.notifiation.port.outbound.persistence.NotificationMessageRepositoryPort;
import notification.application.outbox.port.outbound.MessageOutboxRepositoryPort;
import notification.definition.annotations.UnitOfWork;
import notification.definition.vo.outbox.MessageOutbox;
import notification.domain.NotificationMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageWithOutboxSaver {

    private final NotificationMessageRepositoryPort notificationMessageRepository;
    private final MessageOutboxRepositoryPort MessageOutboxRepository;
    private final JsonPayloadConverterPort jsonPayloadConverter;

    /**
     * NotificationMessage를 저장하고, MessageOutbox를 생성하여 저장합니다.
     *
     * @param message NotificationMessage
     * @return MessageOutbox Mono
     */
    @UnitOfWork
    public Mono<MessageOutbox> save(NotificationMessage message) {
        log.debug("Saving NotificationMessage with ID: {}", message.getMessageId());

        return notificationMessageRepository.save(message).flatMap(saved -> {
            MessageOutbox messageOutbox = MessageOutbox.create(
                    saved.getMessageId().value(),
                    jsonPayloadConverter.toJsonPayload(saved),
                    saved.getScheduledAt());

            return MessageOutboxRepository.save(messageOutbox);
        });
    }

}
