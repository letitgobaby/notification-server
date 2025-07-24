package notification.adapter.db.adapter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;

import lombok.RequiredArgsConstructor;
import notification.adapter.db.RequestOutboxEntity;
import notification.adapter.db.repository.R2dbcRequestOutboxRepository;
import notification.application.outbox.port.outbound.RequestOutboxRepositoryPort;
import notification.definition.enums.OutboxStatus;
import notification.definition.utils.InstantDateTimeBridge;
import notification.definition.vo.outbox.OutboxId;
import notification.definition.vo.outbox.RequestOutbox;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class RequestOutboxRepositoryAdapter implements RequestOutboxRepositoryPort {

    private final R2dbcRequestOutboxRepository r2dbcRequestOutboxRepository;
    private final TransactionalOperator transactionalOperator;
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
    public Flux<RequestOutbox> fetchOutboxToProcess(Instant now, int limit) {
        String instanceId = UUID.randomUUID().toString(); // 현재 인스턴스 ID로 설정

        return transactionalOperator.transactional(updateOutboxForLock(now, limit, instanceId))
                .thenMany(Flux.defer(() -> selectLockedOutbox(instanceId, limit)));
    }

    //
    private Flux<RequestOutbox> selectLockedOutbox(String instanceId, int limit) {
        String query = """
                SELECT * FROM request_outbox
                WHERE instance_id = ?
                ORDER BY created_at ASC
                LIMIT %d
                """.formatted(limit);

        return databaseClient.sql(query)
                .bind(0, instanceId)
                .map((row, metadata) -> {
                    return RequestOutboxEntity.builder()
                            .outboxId(row.get("outbox_id", String.class))
                            .aggregateId(row.get("aggregate_id", String.class))
                            .payload(row.get("payload", String.class))
                            .status(row.get("status", String.class))
                            .processedAt(row.get("processed_at", LocalDateTime.class))
                            .retryAttempts(row.get("retry_attempts", Integer.class))
                            .nextRetryAt(row.get("next_retry_at", LocalDateTime.class))
                            .createdAt(row.get("created_at", LocalDateTime.class))
                            .build();
                })
                .all()
                .map(RequestOutboxEntity::toDomain)
                .switchIfEmpty(Flux.empty());
    }

    //
    private Mono<Long> updateOutboxForLock(Instant now, int limit, String instanceId) {
        String updateQuery = """
                UPDATE request_outbox
                SET instance_id = ?, status = ?, processed_at = ?
                WHERE instance_id IS NULL
                    AND outbox_id IN (
                        SELECT outbox_id FROM (
                        SELECT outbox_id FROM request_outbox
                        WHERE status IN ('PENDING', 'FAILED')
                            AND (next_retry_at IS NULL OR next_retry_at <= ?)
                            AND instance_id IS NULL
                        ORDER BY created_at ASC
                        LIMIT %d
                        ) AS subquery
                    );
                    """.formatted(limit);

        return databaseClient.sql(updateQuery)
                .bind(0, instanceId)
                .bind(1, OutboxStatus.IN_PROGRESS.name())
                .bind(2, InstantDateTimeBridge.toLocalDateTime(now))
                .bind(3, InstantDateTimeBridge.toLocalDateTime(now))
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Long> cleanUpInProgressOutboxs(Instant before) {
        String updateQuery = """
                UPDATE request_outbox
                SET status = ?, instance_id = NULL
                WHERE status = 'IN_PROGRESS'
                    AND instance_id IS NOT NULL
                    AND processed_at < ?;
                """;

        return databaseClient.sql(updateQuery)
                .bind(0, OutboxStatus.PENDING.name())
                .bind(1, InstantDateTimeBridge.toLocalDateTime(before))
                .fetch()
                .rowsUpdated();
    }

}
