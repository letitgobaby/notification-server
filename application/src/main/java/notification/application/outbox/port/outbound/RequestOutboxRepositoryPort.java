package notification.application.outbox.port.outbound;

import notification.definition.vo.outbox.OutboxId;
import notification.definition.vo.outbox.RequestOutbox;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RequestOutboxRepositoryPort {

    /**
     * Saves the outbox message to the repository.
     *
     * @param MessageOutbox the outbox message to save
     * @return the saved outbox message
     */
    Mono<RequestOutbox> save(RequestOutbox domain);

    /**
     * Finds an outbox message by its ID.
     *
     * @param id the ID of the outbox message
     * @return the found outbox message, or null if not found
     */
    Mono<RequestOutbox> findById(OutboxId id);

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

    /**
     * Finds all outbox messages with the specified status.
     *
     * @param status the status of the outbox messages to find
     * @return a Flux of outbox messages matching the status
     */
    Flux<RequestOutbox> findPendingAndFailedMessages();

}
