package notification.application.service.processing.processor;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.inbound.NotificationMessageEventProcessorUseCase;
import notification.application.service.infrastructure.loader.NotificationMessageOutboxLoader;
import notification.application.service.processing.handler.NotificationMessageDispatchHandler;
import notification.application.service.processing.handler.NotificationMessageExceptionHandler;
import notification.definition.annotations.UnitOfWork;
import notification.definition.vo.outbox.MessageOutbox;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationMessageEventProcessor implements NotificationMessageEventProcessorUseCase {

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
                    .doOnSuccess(v -> log.info("Successfully processed NotificationMessage message: {}",
                            outbox.getAggregateId()))
                    .onErrorResume(e -> exceptionHandler.handle(message, outbox, e));
        });
    }

}
