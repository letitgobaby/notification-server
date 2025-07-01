package notification.application.service.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.events.NotificationRequestReceivedEvent;
import notification.application.notifiation.port.inbound.NotificationRequestEventProcessorUseCase;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestReceivedEventListener {

    private final NotificationRequestEventProcessorUseCase notificationRequestEventProcessor;

    /**
     * 알림 요청 수신 이벤트를 수신하고 처리합니다.
     * 이 메서드는 트랜잭션 커밋 후에 호출됩니다.
     *
     * @param event NotificationRequestReceivedEvent
     * @return Mono<Void>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public Mono<Void> listen(NotificationRequestReceivedEvent event) {
        log.info("Received NotificationRequestReceivedEvent: {}", event.getRequestOutbox().getAggregateId());

        return notificationRequestEventProcessor.process(event.getRequestOutbox());
    }

}
