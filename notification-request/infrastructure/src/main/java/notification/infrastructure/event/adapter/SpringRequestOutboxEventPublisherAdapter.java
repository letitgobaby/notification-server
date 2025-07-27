package notification.infrastructure.event.adapter;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.events.NotificationRequestReceivedEvent;
import notification.application.outbox.port.outbound.RequestOutboxEventPublisherPort;
import notification.definition.vo.outbox.RequestOutbox;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringRequestOutboxEventPublisherAdapter implements RequestOutboxEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 아웃박스 메시지를 이벤트로 발행합니다.
     * 스케줄링된 요청은 이벤트를 발행하지 않습니다.
     * 
     * TODO: MQ로 변경 필요, 변경시 Adapter로 이동
     *
     * @param outbox 아웃박스 메시지
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> publish(RequestOutbox outbox) {
        return Mono.fromRunnable(() -> {
            Instant scheduledAt = outbox.getNextRetryAt();
            Instant buffetTime = Instant.now().plusSeconds(5); // 버퍼 타임 설정 (5초)
            if (scheduledAt != null && scheduledAt.isAfter(buffetTime)) {
                return; // 스케줄링된 요청은 이벤트 발행하지 않음
            }

            applicationEventPublisher.publishEvent(new NotificationRequestReceivedEvent(this, outbox));
        });
    }

}
