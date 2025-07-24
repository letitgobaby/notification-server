package notification.infrastructure.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.events.NotificationMessageReadyEvent;
import notification.application.notifiation.port.inbound.NotificationMessageEventProcessorUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringNotificationMessageReadyEventListener {

    private final NotificationMessageEventProcessorUseCase notificationMessageProcessor;

    /**
     * 알림 메시지 준비 이벤트를 수신하고 처리합니다.
     *
     * @param event NotificationMessageReadyEvent
     * @return Mono<Void>
     */
    @EventListener
    public void listen(NotificationMessageReadyEvent event) {
        log.info("Received NotificationMessageReadyEvent: {}", event.getMessageOutbox().getAggregateId());

        notificationMessageProcessor.process(event.getMessageOutbox()).subscribe();
    }
}
