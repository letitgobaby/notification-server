package notification.application.service.support;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.outbound.message.NotificationMessagePublishPort;
import notification.application.notifiation.port.outbound.persistence.NotificationMessageRepositoryPort;
import notification.application.outbox.port.outbound.MessageOutboxRepositoryPort;
import notification.definition.vo.outbox.MessageOutbox;
import notification.domain.NotificationMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageDispatchHandler {

    private final NotificationMessageRepositoryPort notificationMessageRepository;
    private final MessageOutboxRepositoryPort MessageOutboxRepository;
    private final NotificationMessagePublishPort notificationMessagePublish;

    /**
     * 알림 메시지를 발행하고, 아웃박스 메시지를 삭제한 후 알림 메시지 상태를 업데이트합니다.
     *
     * @param message 알림 메시지
     * @param outbox  아웃박스 메시지
     * @return Mono<Void>
     */
    public Mono<Void> handle(NotificationMessage message, MessageOutbox outbox) {
        return notificationMessagePublish.publish(message).flatMap(v -> {

            // 메시지 발행이 성공하면 알림 메시지를 DISPATCHED 상태로 업데이트합니다.
            message.markAsDispatched();

            return handleCompletedMessage(message, outbox);
        });
    }

    /**
     * 알림 메시지 상태를 업데이트하고 아웃박스 메시지를 삭제합니다.
     *
     * @param message 알림 메시지
     * @param outbox  아웃박스 메시지
     * @return Mono<Void>
     */
    private Mono<Void> handleCompletedMessage(NotificationMessage message, MessageOutbox outbox) {
        return Mono.zip(
                notificationMessageRepository.update(message),
                MessageOutboxRepository.deleteById(outbox.getOutboxId()))
                .then();
    }

}
