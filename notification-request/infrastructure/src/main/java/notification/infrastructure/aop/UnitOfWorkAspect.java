package notification.infrastructure.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.definition.annotations.UnitOfWork;
import notification.definition.enums.Propagation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class UnitOfWorkAspect {

    // private final TransactionalOperator transactionalOperator;

    private final ReactiveTransactionManager reactiveTransactionManager;

    @Around("@annotation(notification.definition.annotations.UnitOfWork)")
    public Object wrapWithTransaction(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        log.info(" ** Transaction {} Start for method: {}", signature.getClass().getName(),
                signature.getName());

        UnitOfWork unitOfWorkAnnotation = signature.getMethod().getAnnotation(UnitOfWork.class);

        // 트랜잭션 정의 생성 (전파 속성 설정)
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(mapPropagation(unitOfWorkAnnotation.propagation()));

        TransactionalOperator operator = TransactionalOperator.create(reactiveTransactionManager, def);

        return operator.transactional(
                Mono.defer(() -> { // Mono.defer로 실행을 지연시켜 AOP 실행 순서를 보장
                    try {
                        Object result = pjp.proceed();
                        if (result instanceof Mono<?> mono) {
                            return mono;
                        } else if (result instanceof Flux<?> flux) {
                            return flux.collectList(); // Flux는 Mono로 변환하여 처리 편의성 증대
                        } else {
                            log.warn("@UnitOfWork can only be applied to Mono or Flux return types.");
                            return Mono.justOrEmpty(result); // 비 Reactive 타입도 오류 없이 반환
                        }
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                }).doFinally(signalType -> {
                    log.info(" ** Transaction {} completed for method: {} with signal: {}",
                            signature.getClass().getName(), signature.getName(), signalType);
                }));
    }

    // 우리 커스텀 Propagation Enum을 Spring의 TransactionDefinition Propagation 상수로 매핑
    private int mapPropagation(Propagation propagation) {
        switch (propagation) {
            case REQUIRED:
                return TransactionDefinition.PROPAGATION_REQUIRED;
            case REQUIRES_NEW:
                return TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            // 다른 Propagation 값이 추가되면 여기에 매핑 로직 추가
            default:
                return TransactionDefinition.PROPAGATION_REQUIRED;
        }
    }

}
