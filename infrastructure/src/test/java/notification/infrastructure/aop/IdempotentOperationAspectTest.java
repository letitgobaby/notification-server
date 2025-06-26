package notification.infrastructure.aop;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import notification.application.service.IdempotentOperationService;
import notification.definition.annotations.Idempotent;
import reactor.core.publisher.Mono;

class IdempotentOperationAspectTest {

    IdempotentOperationService idempotentOperationService = mock(IdempotentOperationService.class);
    IdempotentOperationAspect aspect = new IdempotentOperationAspect(idempotentOperationService);

    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    Idempotent idempotent = mock(Idempotent.class);

    static class TestService {
        @Idempotent(argKey = "id", operationType = "testOp")
        public Mono<String> testMethod(String id, String value) {
            return Mono.just("result");
        }

        @Idempotent(argKey = "id")
        public String notMonoMethod(String id) {
            return "fail";
        }

        @Idempotent(argKey = "id")
        public Mono<?> monoRawType(String id) {
            return Mono.just("raw");
        }
    }

    Method testMethod;
    Method notMonoMethod;
    Method monoRawTypeMethod;

    @BeforeEach
    void setUp() throws Exception {
        testMethod = TestService.class.getMethod("testMethod", String.class, String.class);
        notMonoMethod = TestService.class.getMethod("notMonoMethod", String.class);
        monoRawTypeMethod = TestService.class.getMethod("monoRawType", String.class);

        when(pjp.getSignature()).thenReturn(signature);
    }

    @Test
    void applyIdempotency_success() throws Throwable {
        when(signature.getMethod()).thenReturn(testMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id", "value" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc123", "val" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("testOp");

        Mono<String> businessMono = Mono.just("result");
        when(pjp.proceed()).thenReturn(businessMono);

        when(idempotentOperationService.performOperation(
                eq("abc123"),
                eq("testOp"),
                any(Mono.class),
                eq(String.class))).thenReturn(Mono.just("finalResult"));

        Object result = aspect.applyIdempotency(pjp, idempotent);

        assertThat(result).isInstanceOf(Mono.class);
        assertThat(((Mono<?>) result).block()).isEqualTo("finalResult");
    }

    @Test
    void applyIdempotency_throwsIfArgKeyNotFound() {
        when(signature.getMethod()).thenReturn(testMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id", "value" });
        when(pjp.getArgs()).thenReturn(new Object[] { null, "val" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("");

        assertThatThrownBy(() -> aspect.applyIdempotency(pjp, idempotent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency argKey value not found");
    }

    @Test
    void applyIdempotency_throwsIfReturnTypeIsNotMono() {
        when(signature.getMethod()).thenReturn(notMonoMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("");

        assertThatThrownBy(() -> aspect.applyIdempotency(pjp, idempotent))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("@Idempotent only supports methods returning Mono<?>");
    }

    @Test
    void extractMonoGenericType_throwsIfCannotDetermineType() throws Exception {
        when(signature.getMethod()).thenReturn(monoRawTypeMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("");

        assertThatThrownBy(() -> aspect.applyIdempotency(pjp, idempotent))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unable to determine Mono generic return type");
    }
}