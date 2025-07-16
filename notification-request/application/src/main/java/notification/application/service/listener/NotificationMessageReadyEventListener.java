package notification.application.service.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.events.NotificationMessageReadyEvent;
import notification.application.notifiation.port.inbound.NotificationMessageProcessorUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageReadyEventListener {

    private final NotificationMessageProcessorUseCase notificationMessageProcessor;

    /**
     * 알림 메시지 준비 이벤트를 수신하고 처리합니다.
     *
     * @param event NotificationMessageReadyEvent
     * @return Mono<Void>
     */
    // @EventListener
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // public Mono<Void> listen(NotificationMessageReadyEvent event) {
    // log.info("Received NotificationMessageReadyEvent: {}",
    // event.getMessageOutbox().getAggregateId());

    // return notificationMessageProcessor.process(event.getMessageOutbox());
    // }

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void listen(NotificationMessageReadyEvent event) {
        log.info("Received NotificationMessageReadyEvent: {}", event.getMessageOutbox().getAggregateId());

        notificationMessageProcessor.process(event.getMessageOutbox()).subscribe();
    }

}
