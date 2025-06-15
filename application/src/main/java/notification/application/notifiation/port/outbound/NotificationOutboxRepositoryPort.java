package notification.application.notifiation.port.outbound;

import java.time.Instant;

import notification.domain.notification.NotificationOutbox;
import notification.domain.notification.enums.OutboxStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationOutboxRepositoryPort {

    Mono<NotificationOutbox> findById(Long outboxId);

    Flux<NotificationOutbox> findScheduledAndReady(OutboxStatus status, Instant time);

    Mono<NotificationOutbox> save(NotificationOutbox outbox);

    Mono<Void> deleteById(Long outboxId);

    Flux<NotificationOutbox> deleteByStatus(OutboxStatus status);

}
