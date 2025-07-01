package notification.adapter.db.adapter;

import java.time.LocalDateTime;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import notification.adapter.db.RequestOutboxEntity;
import notification.adapter.db.repository.R2dbcRequestOutboxRepository;
import notification.application.outbox.port.outbound.RequestOutboxRepositoryPort;
import notification.definition.vo.outbox.OutboxId;
import notification.definition.vo.outbox.RequestOutbox;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class RequestOutboxRepositoryAdapter implements RequestOutboxRepositoryPort {

    private final R2dbcRequestOutboxRepository r2dbcRequestOutboxRepository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<RequestOutbox> save(RequestOutbox domain) {
        return r2dbcRequestOutboxRepository.save(RequestOutboxEntity.fromDomain(domain))
                .map(RequestOutboxEntity::toDomain);
    }

    @Override
    public Mono<RequestOutbox> findById(OutboxId id) {
        return r2dbcRequestOutboxRepository.findById(id.value())
                .map(RequestOutboxEntity::toDomain);
    }

    @Override
    public Mono<Void> deleteByAggregateId(String aggregateId) {
        return r2dbcRequestOutboxRepository.deleteByAggregateId(aggregateId);
    }

    @Override
    public Mono<Void> deleteById(OutboxId id) {
        return r2dbcRequestOutboxRepository.deleteById(id.value());
    }

    @Override
    public Flux<RequestOutbox> findPendingAndFailedMessages() {
        String query = """
                SELECT * FROM request_outbox
                WHERE status IN ('PENDING', 'FAILED')
                ORDER BY created_at DESC
                LIMIT 1000
                        """;

        return databaseClient.sql(query)
                .map((row, metadata) -> RequestOutboxEntity.builder()
                        .outboxId(row.get("outbox_id", String.class))
                        .aggregateId(row.get("aggregate_id", String.class))
                        .payload(row.get("payload", String.class))
                        .status(row.get("status", String.class))
                        .processedAt(row.get("processed_at", LocalDateTime.class))
                        .retryAttempts(row.get("retry_attempts", Integer.class))
                        .nextRetryAt(row.get("next_retry_at", LocalDateTime.class))
                        .createdAt(row.get("created_at", LocalDateTime.class))
                        .build())
                .all()
                .map(RequestOutboxEntity::toDomain);
    }

}
