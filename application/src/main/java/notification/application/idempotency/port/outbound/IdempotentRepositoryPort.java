package notification.application.idempotency.port.outbound;

import notification.application.idempotency.Idempotency;
import reactor.core.publisher.Mono;

public interface IdempotentRepositoryPort {

    /**
     * 멱등성 키를 저장합니다.
     *
     * @param idempotency 멱등성 객체
     * @return 저장된 멱등성 객체
     */
    Mono<Idempotency> save(Idempotency idempotency);

    /**
     * 멱등성 키를 조회합니다.
     *
     * @param idempotencyKey 멱등성 키
     * @param operationType  멱등성 작업의 타입
     * @return 조회된 멱등성 객체
     */
    Mono<Idempotency> findById(String idempotencyKey, String operationType);

}
