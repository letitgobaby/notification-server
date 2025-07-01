package notification.application.service.support;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.JsonPayloadFactory;
import notification.application.notifiation.port.outbound.persistence.NotificationMessageRepositoryPort;
import notification.application.outbox.port.outbound.MessageOutboxRepositoryPort;
import notification.definition.vo.outbox.MessageOutbox;
import notification.domain.NotificationMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageWithOutboxSaver {

    private final NotificationMessageRepositoryPort notificationMessageRepository;
    private final MessageOutboxRepositoryPort MessageOutboxRepository;
    private final JsonPayloadFactory jsonPayloadFactory;

    /**
     * NotificationMessage를 저장하고, MessageOutbox를 생성하여 저장합니다.
     *
     * @param message NotificationMessage
     * @return MessageOutbox Mono
     */
    public Mono<MessageOutbox> save(NotificationMessage message) {
        return notificationMessageRepository.save(message)
                .flatMap(saved -> {
                    MessageOutbox messageOutbox = MessageOutbox.create(
                            saved.getMessageId().value(),
                            jsonPayloadFactory.toJsonPayload(saved),
                            saved.getScheduledAt());

                    return MessageOutboxRepository.save(messageOutbox);
                });
    }

}
