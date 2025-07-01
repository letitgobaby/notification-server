package notification.adapter.db.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import notification.adapter.db.MessageOutboxEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface R2dbcMessageOutboxRepository extends R2dbcRepository<MessageOutboxEntity, String> {

    /**
     * Find all MessageOutboxEntity by aggregateId.
     *
     * @param aggregateId the aggregate ID to search for
     * @return Flux of MessageOutboxEntity
     */
    Flux<MessageOutboxEntity> findByAggregateId(String aggregateId);

    /**
     * Delete MessageOutboxEntity by aggregateId.
     *
     * @param aggregateId the aggregate ID to delete
     * @return Mono<Void>
     */
    Mono<Void> deleteByAggregateId(String aggregateId);

}