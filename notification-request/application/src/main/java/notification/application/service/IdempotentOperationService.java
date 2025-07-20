package notification.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.JsonPayloadFactory;
import notification.application.idempotency.Idempotency;
import notification.application.idempotency.IdempotencyDuplicateKeyException;
import notification.application.idempotency.port.inbound.IdempotentOperationUseCase;
import notification.application.idempotency.port.outbound.IdempotentRepositoryPort;
import notification.definition.annotations.UnitOfWork;
import notification.definition.enums.Propagation;
import notification.definition.exceptions.DuplicateRequestException;
import notification.definition.vo.JsonPayload;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentOperationService implements IdempotentOperationUseCase {

    private final IdempotentRepositoryPort idempotencyRepository;
    private final JsonPayloadFactory jsonPayloadFactory;

    /**
     * Idempotent operation을 수행합니다.
     * 
     * @param idempotencyKeyIdempotencyKey
     * @param operationType
     * @param data
     * @return Idempotency 객체
     */
    @UnitOfWork(propagation = Propagation.REQUIRES_NEW)
    @Override
    public <T> Mono<T> performOperation(String idempotencyKey, String operationType,
            Mono<T> businessLogic, Class<T> resultType) {
        log.info("Performing idempotent operation: {} / {}", idempotencyKey, operationType);

        return idempotencyRepository.findById(idempotencyKey, operationType)
                .switchIfEmpty(createIdempotency(idempotencyKey, operationType))
                .flatMap(idempotency -> {
                    if (idempotency.data() != null) {
                        // 이미 처리된 경우: 저장된 데이터 반환
                        return getDataAsObject(idempotency.data(), resultType);
                    }

                    // 새로운 요청: 비즈니스 로직 실행 및 결과 저장
                    return executeAndSave(idempotency, businessLogic, resultType);
                })
                .doOnSuccess(result -> log.info("Idempotent operation completed successfully: {} / {}",
                        idempotencyKey, operationType))
                .onErrorResume(e -> {
                    log.error("Failed to perform idempotent operation: {}", e.getMessage(), e);

                    // IdempotencyDuplicateKeyException 처리
                    if (e instanceof IdempotencyDuplicateKeyException) {
                        return retryDuplicateKeyException(idempotencyKey, operationType, resultType);
                    }

                    // 다른 예외 처리 (businessLogic 실행 중 발생한 예외 등)
                    return Mono.error(e);
                });
    }

    /**
     * Idempotent operation을 수행합니다.
     * 
     * @param idempotencyKey
     * @param operationType
     * @param businessLogic
     * @return Mono<T>
     */
    private <T> Mono<T> executeAndSave(Idempotency idempotency, Mono<T> businessLogic, Class<T> resultType) {
        // 1. 비즈니스 로직 실행
        return businessLogic.flatMap(result -> {

            // 2. 결과를 JsonPayload로 변환
            return getDataAsString(result).flatMap(data -> {

                // 3. 멱등성 객체 생성
                Idempotency updatedIdempotency = new Idempotency(
                        idempotency.idempotencyKey(), idempotency.operationType(),
                        data, Instant.now());

                // 4. 생성된 객체 저장 후 결과 반환
                return idempotencyRepository.save(updatedIdempotency)
                        .thenReturn(result)
                        .onErrorResume(e -> {
                            // 4-1. 중복 키 예외 발생 시 재시도
                            if (e instanceof DuplicateKeyException) {
                                return Mono.error(new IdempotencyDuplicateKeyException(
                                        "Idempotency Duplicate key error occurred.", e));
                            }

                            return Mono.error(e);
                        });
            });
        });
    }

    /**
     * 중복 키 예외 발생 시 재시도 로직
     * 
     * @param idempotencyKey
     * @param operationType
     * @param resultType
     * @return Mono<T>
     */
    private <T> Mono<T> retryDuplicateKeyException(String idempotencyKey, String operationType, Class<T> resultType) {
        return idempotencyRepository.findById(idempotencyKey, operationType)
                .flatMap(idempotency -> {
                    if (idempotency.data() != null) {
                        // 기존 데이터를 객체로 변환하여 반환
                        return getDataAsObject(idempotency.data(), resultType);
                    }

                    // 데이터가 없거나 변환 불가능하면 예외 발생 (비정상 케이스)
                    return Mono.error(
                            new DuplicateRequestException("Idempotent operation concurrent conflict with no result."));
                })
                .onErrorResume(e -> {
                    log.error("Retrying idempotent operation due to duplicate key: {}", e.getMessage(), e);
                    return Mono.error(new RuntimeException("Idempotent operation failed after retry.", e));
                });
    }

    //
    private Mono<Idempotency> createIdempotency(String idempotencyKey, String operationType) {
        return Mono.just(new Idempotency(idempotencyKey, operationType, null, Instant.now()));
    }

    //
    private Mono<JsonPayload> getDataAsString(Object data) {
        return Mono.fromCallable(() -> jsonPayloadFactory.toJsonPayload(data));
    }

    //
    private <T> Mono<T> getDataAsObject(JsonPayload JsonPayload, Class<T> type) {
        return Mono.fromCallable(() -> jsonPayloadFactory.fromJsonPayload(JsonPayload, type));
    }

}
