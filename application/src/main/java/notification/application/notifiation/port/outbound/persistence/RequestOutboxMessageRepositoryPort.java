package notification.application.notifiation.port.outbound.persistence;

import notification.domain.RequestOutboxMessage;
import notification.domain.vo.OutboxId;
import reactor.core.publisher.Mono;

public interface RequestOutboxMessageRepositoryPort {

    /**
     * Saves the outbox message to the repository.
     *
     * @param outboxMessage the outbox message to save
     * @return the saved outbox message
     */
    Mono<RequestOutboxMessage> save(RequestOutboxMessage domain);

    /**
     * Finds an outbox message by its ID.
     *
     * @param id the ID of the outbox message
     * @return the found outbox message, or null if not found
     */
    Mono<RequestOutboxMessage> findById(OutboxId id);

    /**
     * Deletes an outbox message by its aggregate ID.
     *
     * @param aggregateId the aggregate ID of the outbox message to delete
     * @return Mono<Void> indicating completion
     */
    Mono<Void> deleteByAggregateId(String aggregateId);

    /**
     * Deletes an outbox message by its ID.
     *
     * @param id the ID of the outbox message to delete
     */
    Mono<Void> deleteById(OutboxId id);

}
