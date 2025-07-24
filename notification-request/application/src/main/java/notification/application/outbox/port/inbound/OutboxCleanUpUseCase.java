package notification.application.outbox.port.inbound;

import java.time.Instant;

import reactor.core.publisher.Mono;

public interface OutboxCleanUpUseCase {

    /**
     * Cleans up in-progress outbox messages that were created before the specified
     * time.
     * This is used to remove messages that are stuck in processing.
     *
     * @param before the time before which in-progress messages should be cleaned up
     * @return Mono<Long> indicating the number of messages cleaned up
     */
    Mono<Void> cleanUpInProgressOutboxs(Instant before);

}