package notification.application.notifiation.port.outbound.persistence;

import notification.domain.OutboxMessage;
import notification.domain.vo.OutboxId;
import reactor.core.publisher.Mono;

public interface OutboxMessageRepositoryPort {
    /**
     * Saves the outbox message to the repository.
     *
     * @param outboxMessage the outbox message to save
     * @return the saved outbox message
     */
    Mono<OutboxMessage> save(OutboxMessage domain);

    /**
     * Updates the outbox message in the repository.
     *
     * @param domain the outbox message to update
     * @return the updated outbox message
     */
    Mono<OutboxMessage> update(OutboxMessage domain);

    /**
     * Finds an outbox message by its ID.
     *
     * @param id the ID of the outbox message
     * @return the found outbox message, or null if not found
     */
    Mono<OutboxMessage> findById(OutboxId id);

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
