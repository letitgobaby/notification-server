package notification.application.service.processor;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.events.NotificationMessageScheduledEvent;
import notification.application.notifiation.events.NotificationRequestReceivedEvent;
import notification.application.notifiation.port.inbound.NotificationRequestEventProcessorUseCase;
import notification.application.service.support.NotificationRequestExecutionHandler;
import notification.application.service.support.NotificationRequestOutboxLoader;
import notification.application.service.support.NotificationRequestProcessingHandler;
import notification.definition.annotations.UnitOfWork;
import notification.domain.OutboxMessage;
import notification.domain.RequestOutboxMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRequestEventProcessor implements NotificationRequestEventProcessorUseCase {

    private final NotificationRequestOutboxLoader notificationRequestOutboxLoader;
    private final NotificationRequestProcessingHandler notificationRequestProcessingHandler;
    private final NotificationRequestExecutionHandler executionHandler;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * NotificationRequest를 기반으로 NotificationMessage 리스트를 생성합니다.
     * 
     * @param request 알림 요청 정보
     * @return 생성된 NotificationMessage 리스트
     */
    @UnitOfWork
    @Override
    public Mono<Void> process(NotificationRequestReceivedEvent event) {
        log.info("Processing NotificationRequestReceivedEvent: {}", event.getOutboxMessage().getAggregateId());

        RequestOutboxMessage requestOutboxMessage = event.getOutboxMessage();

        return notificationRequestOutboxLoader.load(requestOutboxMessage)
                .flatMap(domain -> {
                    return notificationRequestProcessingHandler.handle(domain)
                            .doOnNext(this::triggerScheduledEvent)
                            .then()
                            .onErrorResume(e -> executionHandler.handle(domain, requestOutboxMessage, e));
                });
    }

    /**
     * Outbox 메시지 리스트를 기반으로 즉시 처리 가능한 이벤트를 트리거합니다.
     * 
     * @param outboxMessages Outbox 메시지 리스트
     */
    private void triggerScheduledEvent(List<OutboxMessage> outboxMessages) {
        if (outboxMessages == null || outboxMessages.isEmpty()) {
            log.warn("No outbox messages to trigger scheduled events.");
            return;
        }

        Instant now = Instant.now();
        for (OutboxMessage outboxMessage : outboxMessages) {
            // 즉시 처리되는 경우에만 이벤트 발행
            if (outboxMessage.getNextRetryAt() != null && outboxMessage.getNextRetryAt().isBefore(now)) {
                applicationEventPublisher.publishEvent(
                        new NotificationMessageScheduledEvent(this, outboxMessage));
            }
        }
    }

}
