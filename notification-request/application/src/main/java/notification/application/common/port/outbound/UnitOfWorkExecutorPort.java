package notification.application.common.port.outbound;

import java.util.function.Function;

import notification.definition.enums.Propagation;
import reactor.core.publisher.Mono;

public interface UnitOfWorkExecutorPort {

    /**
     * 기본 REQUIRED 전파 속성으로 트랜잭션을 실행하고, after-commit 액션은 없습니다.
     * 
     * @param transactionalFlow 트랜잭션 내에서 실행될 Mono
     * @param <T>               Publisher의 타입
     * @return 트랜잭션이 적용된 Mono
     */
    <T> Mono<T> execute(Mono<T> transactionalFlow);

    /**
     * 특정 전파 속성으로 트랜잭션을 실행하고, after-commit 액션은 없습니다.
     * 
     * @param transactionalFlow 트랜잭션 내에서 실행될 Mono
     * @param propagation       트랜잭션 전파 속성
     * @param <T>               Publisher의 타입
     * @return 트랜잭션이 적용된 Mono
     */
    <T> Mono<T> execute(Mono<T> transactionalFlow, Propagation propagation);

    /**
     * 기본 REQUIRED 전파 속성으로 트랜잭션을 실행하고, 트랜잭션 성공 커밋 후에 실행될 작업을 정의합니다.
     * 
     * @param transactionalFlow 트랜잭션 내에서 실행될 Mono
     * @param afterCommitAction 트랜잭션 성공 커밋 후에 실행될 Mono<Void>를 반환하는 함수.
     *                          트랜잭션 결과인 T를 인자로 받을 수 있습니다.
     * @param <T>               Publisher의 타입
     * @return 트랜잭션이 적용된 Mono
     */
    <T> Mono<T> execute(Mono<T> transactionalFlow, Function<T, Mono<Void>> afterCommitAction);

    /**
     * 트랜잭션을 실행하고, 트랜잭션이 성공적으로 커밋된 후에 실행될 작업을 정의합니다.
     *
     * @param transactionalFlow 트랜잭션 내에서 실행될 Mono 또는 Flux (Publisher)
     * @param propagation       트랜잭션 전파 속성
     * @param afterCommitAction 트랜잭션 성공 커밋 후에 실행될 Mono<Void>를 반환하는 함수.
     *                          트랜잭션 결과인 T를 인자로 받을 수 있습니다. (null일 수 있음)
     * @param <T>               Publisher의 타입
     * @return 트랜잭션이 적용된 Publisher
     */
    <T> Mono<T> execute(Mono<T> transactionalFlow, Propagation propagation, Function<T, Mono<Void>> afterCommitAction);

}
