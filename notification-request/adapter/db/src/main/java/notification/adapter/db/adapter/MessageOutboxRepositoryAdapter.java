package notification.adapter.db.adapter;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import notification.adapter.db.MessageOutboxEntity;
import notification.adapter.db.repository.R2dbcMessageOutboxRepository;
import notification.application.outbox.port.outbound.MessageOutboxRepositoryPort;
import notification.definition.vo.outbox.MessageOutbox;
import notification.definition.vo.outbox.OutboxId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class MessageOutboxRepositoryAdapter implements MessageOutboxRepositoryPort {

    private final R2dbcMessageOutboxRepository r2dbcMessageOutboxRepository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<MessageOutbox> save(MessageOutbox domain) {
        return r2dbcMessageOutboxRepository.save(MessageOutboxEntity.fromDomain(domain))
                .map(MessageOutboxEntity::toDomain);
    }

    @Override
    public Mono<MessageOutbox> update(MessageOutbox domain) {
        return r2dbcMessageOutboxRepository.save(MessageOutboxEntity.fromDomain(domain))
                .map(MessageOutboxEntity::toDomain);
    }

    @Override
    public Mono<MessageOutbox> findById(OutboxId id) {
        return r2dbcMessageOutboxRepository.findById(id.value())
                .map(MessageOutboxEntity::toDomain);
    }

    @Override
    public Mono<Void> deleteByAggregateId(String aggregateId) {
        return r2dbcMessageOutboxRepository.deleteByAggregateId(aggregateId);
    }

    @Override
    public Mono<Void> deleteById(OutboxId id) {
        return r2dbcMessageOutboxRepository.deleteById(id.value());
    }

    @Override
    public Flux<MessageOutbox> findByAggregateId(String aggregateId) {
        return r2dbcMessageOutboxRepository.findByAggregateId(aggregateId)
                .map(MessageOutboxEntity::toDomain)
                .switchIfEmpty(Flux.empty());
    }

    @Override
    public Flux<MessageOutbox> findPendingAndFailedMessages() {
        String query = """
                SELECT * FROM message_outbox
                WHERE status IN ('PENDING', 'FAILED')
                ORDER BY created_at DESC
                LIMIT 1000
                        """;

        return databaseClient.sql(query)
                .map((row, metadata) -> MessageOutboxEntity.builder()
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
                .map(MessageOutboxEntity::toDomain)
                .switchIfEmpty(Flux.empty());
    }

    private Flux<MessageOutbox> fetchOutboxToProcess(Instant now, int limit) {
        String query = """
                    UPDATE request_outbox
                    SET status = 'IN_PROGRESS'
                    WHERE status IN ('PENDING', 'FAILED')
                      AND next_retry_at <= :now
                    ORDER BY created_at
                    LIMIT :limit
                    RETURNING *;
                """;

        return databaseClient.sql(query)
                .bind("now", now)
                .bind("limit", limit)
                .map((row, metadata) -> MessageOutboxEntity.builder()
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
                .map(MessageOutboxEntity::toDomain)
                .switchIfEmpty(Flux.empty());
    }

}
