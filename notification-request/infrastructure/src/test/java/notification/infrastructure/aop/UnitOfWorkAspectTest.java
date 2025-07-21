package notification.infrastructure.aop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.ReactiveTransactionManager;

import notification.definition.annotations.UnitOfWork;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class UnitOfWorkAspectTest {

    private ReactiveTransactionManager reactiveTransactionManager;
    private UnitOfWorkAspect aspect;
    private ProceedingJoinPoint pjp;
    private MethodSignature methodSignature;
    private Method method;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        reactiveTransactionManager = mock(ReactiveTransactionManager.class);
        pjp = mock(ProceedingJoinPoint.class);
        methodSignature = mock(MethodSignature.class);

        // 테스트용 메서드 생성 (UnitOfWork 어노테이션이 있는)
        method = TestService.class.getMethod("testMethod");
        when(methodSignature.getMethod()).thenReturn(method);
        when(pjp.getSignature()).thenReturn(methodSignature);
    }

    // 테스트용 서비스 클래스
    static class TestService {
        @UnitOfWork
        public Mono<String> testMethod() {
            return Mono.just("test");
        }
    }

    @Test
    void constructor_shouldCreateAspectWithTransactionManager() {
        // Given & When
        aspect = new UnitOfWorkAspect(reactiveTransactionManager);

        // Then
        assertNotNull(aspect);
    }

    @Test
    void wrapWithTransaction_shouldReturnMono() throws Throwable {
        // Given
        aspect = new UnitOfWorkAspect(reactiveTransactionManager);
        Mono<String> mono = Mono.just("test");
        when(pjp.proceed()).thenReturn(mono);

        // When
        Object result = aspect.wrapWithTransaction(pjp);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof Mono);
    }

    @Test
    void wrapWithTransaction_shouldReturnMonoForFlux() throws Throwable {
        // Given
        aspect = new UnitOfWorkAspect(reactiveTransactionManager);
        Flux<String> flux = Flux.just("a", "b");
        when(pjp.proceed()).thenReturn(flux);

        // When
        Object result = aspect.wrapWithTransaction(pjp);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof Mono);
    }

    @Test
    void wrapWithTransaction_shouldReturnMonoForNonReactiveType() throws Throwable {
        // Given
        aspect = new UnitOfWorkAspect(reactiveTransactionManager);
        String value = "not reactive";
        when(pjp.proceed()).thenReturn(value);

        // When
        Object result = aspect.wrapWithTransaction(pjp);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof Mono);
    }

    @Test
    void wrapWithTransaction_shouldReturnMonoOnException() throws Throwable {
        // Given
        aspect = new UnitOfWorkAspect(reactiveTransactionManager);
        RuntimeException ex = new RuntimeException("fail");
        when(pjp.proceed()).thenThrow(ex);

        // When
        Object result = aspect.wrapWithTransaction(pjp);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        // 실제로 에러가 발생하는지는 별도로 검증하지 않음 (TransactionManager mock이 실제 동작하지 않으므로)
    }
}