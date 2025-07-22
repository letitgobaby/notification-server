package notification.application.outbox.port.outbound;

import java.time.Instant;

import notification.definition.vo.outbox.MessageOutbox;
import notification.definition.vo.outbox.OutboxId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageOutboxRepositoryPort {
    /**
     * Saves the outbox message to the repository.
     *
     * @param MessageOutbox the outbox message to save
     * @return the saved outbox message
     */
    Mono<MessageOutbox> save(MessageOutbox domain);

    /**
     * Finds an outbox message by its ID.
     *
     * @param id the ID of the outbox message
     * @return the found outbox message, or null if not found
     */
    Mono<MessageOutbox> findById(OutboxId id);

    /**
     * Finds an outbox message by its aggregate ID.
     *
     * @param aggregateId the aggregate ID of the outbox message
     * @return Flux of outbox messages matching the aggregate ID
     */
    Flux<MessageOutbox> findByAggregateId(String aggregateId);

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
     * Fetches outbox messages that are ready to be processed.
     * This method updates the status of the messages to IN_PROGRESS
     * and returns a limited number of messages for processing.
     *
     * @param now   the current time to check against next retry times
     * @param limit the maximum number of messages to fetch
     * @return a Flux of outbox messages ready for processing
     */
    Flux<MessageOutbox> fetchOutboxToProcess(Instant now, int limit);

}
