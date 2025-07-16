package notification.infrastructure.aop;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import notification.application.service.IdempotentOperationService;
import notification.definition.annotations.Idempotent;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class IdempotentOperationAspectTest {

    @Mock
    private IdempotentOperationService idempotentOperationService;

    @InjectMocks
    private IdempotentOperationAspect aspect;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private MethodSignature signature;

    @Mock
    private Idempotent idempotent;

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

        @Idempotent(argKey = "id")
        public Mono<Integer> integerMonoMethod(String id) {
            return Mono.just(42);
        }

        @Idempotent(argKey = "customKey")
        public Mono<String> customKeyMethod(String customKey, String otherParam) {
            return Mono.just("customResult");
        }
    }

    Method testMethod;
    Method notMonoMethod;
    Method monoRawTypeMethod;
    Method integerMonoMethod;
    Method customKeyMethod;

    @BeforeEach
    void setUp() throws Exception {
        testMethod = TestService.class.getMethod("testMethod", String.class, String.class);
        notMonoMethod = TestService.class.getMethod("notMonoMethod", String.class);
        monoRawTypeMethod = TestService.class.getMethod("monoRawType", String.class);
        integerMonoMethod = TestService.class.getMethod("integerMonoMethod", String.class);
        customKeyMethod = TestService.class.getMethod("customKeyMethod", String.class, String.class);

        when(pjp.getSignature()).thenReturn(signature);
    }

    @Test
    void applyIdempotency_success() throws Throwable {
        // Given
        when(signature.getMethod()).thenReturn(testMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id", "value" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc123", "val" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("testOp");

        Mono<String> businessMono = Mono.just("result");
        when(pjp.proceed()).thenReturn(businessMono);

        // Mock service to execute the business logic and return result
        when(idempotentOperationService.performOperation(
                eq("abc123"),
                eq("testOp"),
                any(),
                eq(String.class)))
                .thenAnswer(invocation -> {
                    Mono<String> businessLogic = invocation.getArgument(2);
                    return businessLogic.map(result -> "finalResult");
                });

        // When
        // Object result = aspect.applyIdempotency(pjp, idempotent);
        Object result = aspect.applyIdempotency(idempotent, pjp);

        // Then
        assertThat(result).isInstanceOf(Mono.class);
        assertThat(((Mono<?>) result).block()).isEqualTo("finalResult");
    }

    @Test
    void applyIdempotency_successWithEmptyOperationType() throws Throwable {
        // Given
        when(signature.getMethod()).thenReturn(testMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id", "value" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc123", "val" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn(""); // Empty operation type

        Mono<String> businessMono = Mono.just("result");
        when(pjp.proceed()).thenReturn(businessMono);

        when(idempotentOperationService.performOperation(
                eq("abc123"),
                eq("testMethod"), // Should use method name
                any(),
                eq(String.class)))
                .thenAnswer(invocation -> {
                    Mono<String> businessLogic = invocation.getArgument(2);
                    return businessLogic.map(result -> "finalResult");
                });

        // When
        // Object result = aspect.applyIdempotency(pjp, idempotent);
        Object result = aspect.applyIdempotency(idempotent, pjp);

        // Then
        assertThat(result).isInstanceOf(Mono.class);
        assertThat(((Mono<?>) result).block()).isEqualTo("finalResult");
    }

    @Test
    void applyIdempotency_successWithIntegerType() throws Throwable {
        // Given
        when(signature.getMethod()).thenReturn(integerMonoMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc123" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("intOp");

        Mono<Integer> businessMono = Mono.just(42);
        when(pjp.proceed()).thenReturn(businessMono);

        when(idempotentOperationService.performOperation(
                eq("abc123"),
                eq("intOp"),
                any(),
                eq(Integer.class)))
                .thenAnswer(invocation -> {
                    Mono<Integer> businessLogic = invocation.getArgument(2);
                    return businessLogic.map(result -> 100);
                });

        // When
        // Object result = aspect.applyIdempotency(pjp, idempotent);
        Object result = aspect.applyIdempotency(idempotent, pjp);

        // Then
        assertThat(result).isInstanceOf(Mono.class);
        assertThat(((Mono<?>) result).block()).isEqualTo(100);
    }

    @Test
    void applyIdempotency_successWithCustomKey() throws Throwable {
        // Given
        when(signature.getMethod()).thenReturn(customKeyMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "customKey", "otherParam" });
        when(pjp.getArgs()).thenReturn(new Object[] { "myCustomKey", "other" });
        when(idempotent.argKey()).thenReturn("customKey");
        when(idempotent.operationType()).thenReturn("customOp");

        Mono<String> businessMono = Mono.just("customResult");
        when(pjp.proceed()).thenReturn(businessMono);

        when(idempotentOperationService.performOperation(
                eq("myCustomKey"),
                eq("customOp"),
                any(),
                eq(String.class)))
                .thenAnswer(invocation -> {
                    Mono<String> businessLogic = invocation.getArgument(2);
                    return businessLogic.map(result -> "finalCustomResult");
                });

        // When
        // Object result = aspect.applyIdempotency(pjp, idempotent);
        Object result = aspect.applyIdempotency(idempotent, pjp);

        // Then
        assertThat(result).isInstanceOf(Mono.class);
        assertThat(((Mono<?>) result).block()).isEqualTo("finalCustomResult");
    }

    @Test
    void applyIdempotency_throwsIfArgKeyNotFound() {
        // Given
        when(signature.getMethod()).thenReturn(testMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id", "value" });
        when(pjp.getArgs()).thenReturn(new Object[] { null, "val" });
        when(idempotent.argKey()).thenReturn("id");

        // When & Then
        // assertThatThrownBy(() -> aspect.applyIdempotency(pjp, idempotent))
        assertThatThrownBy(() -> aspect.applyIdempotency(idempotent, pjp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency argKey value not found: id");
    }

    @Test
    void applyIdempotency_throwsIfArgKeyNotFoundInParameters() {
        // Given
        when(signature.getMethod()).thenReturn(testMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id", "value" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc123", "val" });
        when(idempotent.argKey()).thenReturn("nonExistentKey");

        // When & Then
        // assertThatThrownBy(() -> aspect.applyIdempotency(pjp, idempotent))
        assertThatThrownBy(() -> aspect.applyIdempotency(idempotent, pjp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency argKey value not found: nonExistentKey");
    }

    @Test
    void applyIdempotency_throwsIfReturnTypeIsNotMono() {
        // Given
        when(signature.getMethod()).thenReturn(notMonoMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("");

        // When & Then
        // assertThatThrownBy(() -> aspect.applyIdempotency(pjp, idempotent))
        assertThatThrownBy(() -> aspect.applyIdempotency(idempotent, pjp))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("@Idempotent only supports methods returning Mono<?>");
    }

    @Test
    void applyIdempotency_throwsIfCannotDetermineGenericType() {
        // Given
        when(signature.getMethod()).thenReturn(monoRawTypeMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("");

        // When & Then
        // assertThatThrownBy(() -> aspect.applyIdempotency(pjp, idempotent))
        assertThatThrownBy(() -> aspect.applyIdempotency(idempotent, pjp))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unable to determine Mono generic return type");
    }

    @Test
    void applyIdempotency_handlesBusinessLogicException() throws Throwable {
        // Given
        when(signature.getMethod()).thenReturn(testMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id", "value" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc123", "val" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("testOp");

        RuntimeException businessException = new RuntimeException("Business logic failed");
        when(pjp.proceed()).thenThrow(businessException);

        when(idempotentOperationService.performOperation(
                eq("abc123"),
                eq("testOp"),
                any(),
                eq(String.class)))
                .thenAnswer(invocation -> {
                    Mono<String> businessLogic = invocation.getArgument(2);
                    return businessLogic;
                });

        // When
        // Object result = aspect.applyIdempotency(pjp, idempotent);
        Object result = aspect.applyIdempotency(idempotent, pjp);

        // Then
        assertThat(result).isInstanceOf(Mono.class);
        assertThatThrownBy(() -> ((Mono<?>) result).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Business logic failed");
    }

    @Test
    void applyIdempotency_handlesIdempotentServiceException() throws Throwable {
        // Given
        when(signature.getMethod()).thenReturn(testMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id", "value" });
        when(pjp.getArgs()).thenReturn(new Object[] { "abc123", "val" });
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("testOp");

        RuntimeException serviceException = new RuntimeException("Service failed");
        when(idempotentOperationService.performOperation(
                eq("abc123"),
                eq("testOp"),
                any(),
                eq(String.class))).thenReturn(Mono.error(serviceException));

        // When
        // Object result = aspect.applyIdempotency(pjp, idempotent);
        Object result = aspect.applyIdempotency(idempotent, pjp);

        // Then
        assertThat(result).isInstanceOf(Mono.class);
        assertThatThrownBy(() -> ((Mono<?>) result).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service failed");
    }

    @Test
    void applyIdempotency_convertsIdempotencyKeyToString() throws Throwable {
        // Given
        when(signature.getMethod()).thenReturn(integerMonoMethod);
        when(signature.getParameterNames()).thenReturn(new String[] { "id" });
        when(pjp.getArgs()).thenReturn(new Object[] { 12345 }); // Integer key
        when(idempotent.argKey()).thenReturn("id");
        when(idempotent.operationType()).thenReturn("intOp");

        Mono<Integer> businessMono = Mono.just(42);
        when(pjp.proceed()).thenReturn(businessMono);

        when(idempotentOperationService.performOperation(
                eq("12345"), // Should be converted to string
                eq("intOp"),
                any(),
                eq(Integer.class)))
                .thenAnswer(invocation -> {
                    Mono<Integer> businessLogic = invocation.getArgument(2);
                    return businessLogic.map(result -> 100);
                });

        // When
        // Object result = aspect.applyIdempotency(pjp, idempotent);
        Object result = aspect.applyIdempotency(idempotent, pjp);

        // Then
        assertThat(result).isInstanceOf(Mono.class);
        assertThat(((Mono<?>) result).block()).isEqualTo(100);
    }
}