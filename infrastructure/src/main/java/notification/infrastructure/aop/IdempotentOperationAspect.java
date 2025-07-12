package notification.infrastructure.aop;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.service.IdempotentOperationService;
import notification.definition.annotations.Idempotent;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Aspect의 우선순위를 가장 높게 설정
@RequiredArgsConstructor
public class IdempotentOperationAspect {

    private final IdempotentOperationService idempotentOperationService;

    @Around("@annotation(idempotent)")
    @SuppressWarnings("unchecked")
    public <T> Object applyIdempotency(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Object[] args = pjp.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = signature.getParameterNames();
        for (int i = 0; i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // 1. 키 추출 (SpEL 또는 파라미터명 기준)
        Object idempotencyKey = context.lookupVariable(idempotent.argKey());
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency argKey value not found: " + idempotent.argKey());
        }

        String operationType = idempotent.operationType().isEmpty() ? method.getName() : idempotent.operationType();

        // 2. 반환 타입 확인
        if (!Mono.class.isAssignableFrom(method.getReturnType())) {
            throw new UnsupportedOperationException("@Idempotent only supports methods returning Mono<?>");
        }

        // 3. 반환 타입 내부의 실제 제네릭 타입 추출
        Class<T> resultType = (Class<T>) extractMonoGenericType(method);

        // 4. 비즈니스 로직 지연 실행
        Mono<T> businessLogic = Mono.defer(() -> {
            try {
                return (Mono<T>) pjp.proceed();
            } catch (Throwable e) {
                return Mono.error(e);
            }
        });

        // 5. 멱등성 처리 적용
        return idempotentOperationService.performOperation(
                idempotencyKey.toString(),
                operationType,
                businessLogic,
                resultType);
    }

    /**
     * Mono<T>의 T 타입 추출
     */
    private Class<?> extractMonoGenericType(Method method) {
        Type returnType = method.getGenericReturnType();

        if (returnType instanceof ParameterizedType parameterizedType) {
            Type actualType = parameterizedType.getActualTypeArguments()[0];
            if (actualType instanceof Class<?> clazz) {
                return clazz;
            } else if (actualType instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> raw) {
                return raw;
            }
        }

        throw new UnsupportedOperationException(
                "Unable to determine Mono generic return type for method: " + method.getName());
    }

}
