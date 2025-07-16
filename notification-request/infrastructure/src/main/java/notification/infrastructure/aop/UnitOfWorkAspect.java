package notification.infrastructure.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class UnitOfWorkAspect {

    private final TransactionalOperator transactionalOperator;

    @Around("@annotation(UnitOfWork)")
    public Object wrapWithTransaction(ProceedingJoinPoint pjp) {
        try {
            Object result = pjp.proceed();

            if (result instanceof Mono<?> mono) {
                return transactionalOperator.transactional(mono);
            } else if (result instanceof Flux<?> flux) {
                return transactionalOperator.transactional(flux);
            } else {
                log.warn("@UnitOfWork can only be applied to Mono or Flux return types.");
                return result;
            }
        } catch (Throwable e) {
            return Mono.error(e);
        }
    }

}
