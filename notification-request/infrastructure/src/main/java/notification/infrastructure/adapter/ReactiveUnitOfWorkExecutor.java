package notification.infrastructure.adapter;

import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.port.outbound.UnitOfWorkExecutorPort;
import notification.definition.enums.Propagation;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveUnitOfWorkExecutor implements UnitOfWorkExecutorPort {

    private final ReactiveTransactionManager transactionManager;

    @Override
    public <T> Mono<T> execute(Mono<T> transactionalFlow) {
        return execute(transactionalFlow, Propagation.REQUIRED, null);
    }

    @Override
    public <T> Mono<T> execute(Mono<T> transactionalFlow, Propagation propagation) {
        return execute(transactionalFlow, propagation, null);
    }

    @Override
    public <T> Mono<T> execute(Mono<T> transactionalFlow, Function<T, Mono<Void>> afterCommitAction) {
        return execute(transactionalFlow, Propagation.REQUIRED, afterCommitAction);
    }

    @Override
    public <T> Mono<T> execute(Mono<T> transactionalFlow, Propagation propagation,
            Function<T, Mono<Void>> afterCommitAction) {
        // 트랜잭션 정의 생성
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(mapPropagation(propagation));

        // afterCommitAction이 제공되면 doOnSuccess 훅을 추가합니다.
        if (afterCommitAction != null) {
            // transactional(Mono)의 결과가 T이므로, doOnSuccess의 인자도 T
            return createTransactionalMono(transactionalFlow, def).doOnSuccess(result -> {
                log.debug("Executing afterCommitAction for transaction completion.");

                // afterCommitAction 실행. Mono<Void>이므로 .subscribe()로 트리거
                afterCommitAction.apply(result)
                        .doOnSuccess(unused -> log.debug("AfterCommitAction completed successfully."))
                        .doOnError(e -> log.error("Error during afterCommitAction: {}",
                                e.getMessage(), e))
                        .subscribe();
            });
        }

        return createTransactionalMono(transactionalFlow, def);
    }

    /**
     * 트랜잭션을 생성하고 실행하는 헬퍼 메서드입니다.
     * 
     * @param transactionalFlow 트랜잭션 내에서 실행될 Mono
     * @param def               트랜잭션 정의
     * @param <T>               Publisher의 타입
     * @return 트랜잭션이 적용된 Mono
     */
    private <T> Mono<T> createTransactionalMono(Mono<T> transactionalFlow, DefaultTransactionDefinition def) {
        log.debug("Creating transactional Mono with propagation: {}", def.getPropagationBehavior());

        TransactionalOperator operator = TransactionalOperator.create(transactionManager, def);

        // Mono.defer로 실행을 지연시켜 AOP 없는 명시적 호출에서도 지연을 보장합니다.
        return operator.transactional(Mono.defer(() -> transactionalFlow))
                .doFinally(signalType -> {
                    // 트랜잭션 시작/종료 로깅 (디버깅용)
                    // AOP에서 옮겨온 로직, 여기서는 어떤 방식으로 트랜잭션이 완료되었는지 확인 가능
                    log.debug("Transaction for flow {} completed with signal: {}",
                            transactionalFlow.getClass().getSimpleName(), signalType);
                });
    }

    // Spring의 TransactionDefinition Propagation 상수로 매핑
    private int mapPropagation(Propagation propagation) {
        if (propagation == null) {
            return TransactionDefinition.PROPAGATION_REQUIRED;
        }

        switch (propagation) {
            case REQUIRED:
                return TransactionDefinition.PROPAGATION_REQUIRED;
            case REQUIRES_NEW:
                return TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            default:
                // 기본값으로 REQUIRED를 사용하므로, 여기에 도달할 일은 거의 없습니다.
                return TransactionDefinition.PROPAGATION_REQUIRED;
        }
    }

}
