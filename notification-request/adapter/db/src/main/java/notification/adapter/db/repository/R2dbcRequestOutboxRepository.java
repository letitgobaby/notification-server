package notification.adapter.db.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import notification.adapter.db.RequestOutboxEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface R2dbcRequestOutboxRepository extends R2dbcRepository<RequestOutboxEntity, String> {

    /**
     * Find all RequestOutboxEntity by aggregateId.
     *
     * @param aggregateId the aggregate ID to search for
     * @return Flux of RequestOutboxEntity
     */
    Flux<RequestOutboxEntity> findByAggregateId(String aggregateId);

    /**
     * Delete RequestOutboxEntity by aggregateId.
     *
     * @param aggregateId the aggregate ID to delete
     * @return Mono<Void>
     */
    Mono<Void> deleteByAggregateId(String aggregateId);

}