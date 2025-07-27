package notification.adapter.db.adapter;

import java.time.LocalDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import notification.adapter.db.IdempotencyEntity;
import notification.adapter.db.repository.R2dbcIdempotencyRepository;
import notification.application.idempotency.Idempotency;
import notification.application.idempotency.port.outbound.IdempotentRepositoryPort;
import notification.definition.exceptions.DuplicateRequestException;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class IdempotentRepositoryAdapter implements IdempotentRepositoryPort {

    private final DatabaseClient databaseClient;
    private final R2dbcIdempotencyRepository r2dbcIdempotencyRepository;

    @Override
    public Mono<Idempotency> save(Idempotency idempotency) {
        return Mono.fromCallable(() -> IdempotencyEntity.fromDomain(idempotency))
                .flatMap(r2dbcIdempotencyRepository::save)
                .map(savedEntity -> savedEntity.toDomain())
                .onErrorResume(e -> {
                    if (e instanceof DuplicateKeyException) {
                        return Mono.error(new DuplicateRequestException(
                                "Idempotency key already exists"));
                    }

                    return Mono.error(e);
                });
    }

    @Override
    public Mono<Idempotency> findById(String idempotencyKey, String operationType) {
        String sql = """
                SELECT * FROM idempotency_key
                WHERE idempotency_key = :idempotencyKey
                    AND operation_type = :operationType
                        """;

        return databaseClient.sql(sql)
                .bind("idempotencyKey", idempotencyKey)
                .bind("operationType", operationType)
                .map(row -> new IdempotencyEntity(
                        row.get("idempotency_key", String.class),
                        row.get("operation_type", String.class),
                        row.get("data", String.class),
                        row.get("created_at", LocalDateTime.class)))
                .one()
                .map(IdempotencyEntity::toDomain)
                .switchIfEmpty(Mono.empty());
    }

}
