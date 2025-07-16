package notification.infrastructure.aop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UnitOfWorkAspectTest {

    private TransactionalOperator transactionalOperator;
    private UnitOfWorkAspect aspect;
    private ProceedingJoinPoint pjp;

    @BeforeEach
    void setUp() {
        transactionalOperator = mock(TransactionalOperator.class);
        aspect = new UnitOfWorkAspect(transactionalOperator);
        pjp = mock(ProceedingJoinPoint.class);
    }

    @Test
    void wrapWithTransaction_shouldWrapMonoWithTransaction() throws Throwable {
        Mono<String> mono = Mono.just("test");
        Mono<String> transactionalMono = Mono.just("transactional");
        when(pjp.proceed()).thenReturn(mono);
        when(transactionalOperator.transactional(mono)).thenReturn(transactionalMono);

        Object result = aspect.wrapWithTransaction(pjp);

        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<String>) result)
                .expectNext("transactional")
                .verifyComplete();
        verify(transactionalOperator).transactional(mono);
    }

    @Test
    void wrapWithTransaction_shouldWrapFluxWithTransaction() throws Throwable {
        Flux<String> flux = Flux.just("a", "b");
        Flux<String> transactionalFlux = Flux.just("x", "y");
        when(pjp.proceed()).thenReturn(flux);
        when(transactionalOperator.transactional(flux)).thenReturn(transactionalFlux);

        Object result = aspect.wrapWithTransaction(pjp);

        assertTrue(result instanceof Flux);
        StepVerifier.create((Flux<String>) result)
                .expectNext("x", "y")
                .verifyComplete();
        verify(transactionalOperator).transactional(flux);
    }

    @Test
    void wrapWithTransaction_shouldReturnOriginalResultForNonReactiveType() throws Throwable {
        String value = "not reactive";
        when(pjp.proceed()).thenReturn(value);

        Object result = aspect.wrapWithTransaction(pjp);

        assertEquals(value, result);
        verify(transactionalOperator, never()).transactional(any(Mono.class));
        verify(transactionalOperator, never()).transactional(any(Flux.class));
    }

    @Test
    void wrapWithTransaction_shouldReturnMonoErrorOnException() throws Throwable {
        RuntimeException ex = new RuntimeException("fail");
        when(pjp.proceed()).thenThrow(ex);

        Object result = aspect.wrapWithTransaction(pjp);

        assertTrue(result instanceof Mono);
        StepVerifier.create((Mono<?>) result)
                .expectErrorMatches(e -> e == ex)
                .verify();
    }
}