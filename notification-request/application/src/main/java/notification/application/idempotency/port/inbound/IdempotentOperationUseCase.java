package notification.application.idempotency.port.inbound;

import reactor.core.publisher.Mono;

public interface IdempotentOperationUseCase {

    <T> Mono<T> performOperation(String idempotencyKey, String operationType,
            Mono<T> businessLogic, Class<T> resultType);

}
