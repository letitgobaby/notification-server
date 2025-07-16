package notification.application.service.processor;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.inbound.NotificationMessageProcessorUseCase;
import notification.application.service.support.NotificationMessageDispatchHandler;
import notification.application.service.support.NotificationMessageExceptionHandler;
import notification.application.service.support.NotificationMessageOutboxLoader;
import notification.definition.annotations.UnitOfWork;
import notification.definition.vo.outbox.MessageOutbox;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationMessageProcessor implements NotificationMessageProcessorUseCase {

    private final NotificationMessageOutboxLoader notificationMessageOutboxLoader;
    private final NotificationMessageDispatchHandler notificationMessageDispatchHandler;
    private final NotificationMessageExceptionHandler exceptionHandler;

    /**
     * 알림 메시지를 처리합니다. Outbox 메시지를 로드하고, 알림 메시지를 발행한 후
     * 아웃박스 메시지를 삭제하고 알림 메시지 상태를 업데이트합니다.
     * 
     * @param outbox MessageOutbox
     * @return 처리 결과
     */
    @UnitOfWork
    @Override
    public Mono<Void> process(MessageOutbox outbox) {
        log.info("Processing NotificationMessage with outbox: {}", outbox.getAggregateId());

        return notificationMessageOutboxLoader.load(outbox).flatMap(message -> {
            return notificationMessageDispatchHandler.handle(message, outbox)
                    .onErrorResume(e -> exceptionHandler.handle(message, outbox, e));
        }).doOnSuccess(v -> log.info("Successfully processed message: {}", outbox.getAggregateId()));
    }

}
