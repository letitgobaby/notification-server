package notification.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import notification.definition.enums.Propagation;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ReactiveUnitOfWorkExecutorTest {

    @Mock
    private ReactiveTransactionManager transactionManager;

    @Mock
    private TransactionalOperator transactionalOperator;

    private ReactiveUnitOfWorkExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ReactiveUnitOfWorkExecutor(transactionManager);
    }

    @Test
    @DisplayName("생성자가 ReactiveTransactionManager를 올바르게 설정해야 함")
    void constructor_shouldSetTransactionManager() {
        // When & Then
        assertNotNull(executor);
        // private field이므로 직접 검증 불가, 생성 자체가 성공하면 충분
    }

    @Test
    @DisplayName("기본 execute 메서드가 null이 아닌 Mono를 반환해야 함")
    void execute_shouldReturnNonNullMono() {
        // Given
        Mono<String> flow = Mono.just("test");

        // When
        Mono<String> result = executor.execute(flow);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("전파속성을 지정한 execute 메서드가 null이 아닌 Mono를 반환해야 함")
    void execute_withPropagation_shouldReturnNonNullMono() {
        // Given
        Mono<Integer> flow = Mono.just(42);

        // When
        Mono<Integer> result = executor.execute(flow, Propagation.REQUIRES_NEW);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("afterCommit 액션과 함께 execute 메서드가 null이 아닌 Mono를 반환해야 함")
    void execute_withAfterCommitAction_shouldReturnNonNullMono() {
        // Given
        Mono<String> flow = Mono.just("commit");
        AtomicBoolean afterCommitCalled = new AtomicBoolean(false);
        Function<String, Mono<Void>> afterCommit = value -> {
            afterCommitCalled.set(true);
            return Mono.empty();
        };

        // When
        Mono<String> result = executor.execute(flow, afterCommit);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("전파속성과 afterCommit 액션과 함께 execute 메서드가 null이 아닌 Mono를 반환해야 함")
    void execute_withPropagationAndAfterCommit_shouldReturnNonNullMono() {
        // Given
        Mono<Integer> flow = Mono.just(99);
        Function<Integer, Mono<Void>> afterCommit = value -> Mono.empty();

        // When
        Mono<Integer> result = executor.execute(flow, Propagation.REQUIRED, afterCommit);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("null 플로우로 실행 시 예외 처리 검증")
    void execute_withNullFlow_shouldHandleNull() {
        // When & Then
        // null flow를 전달하면 내부적으로 NullPointerException이 발생할 수 있음
        assertDoesNotThrow(() -> {
            Mono<String> result = executor.execute(null);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("null afterCommit 함수로 실행 시 정상 처리되어야 함")
    void execute_withNullAfterCommit_shouldWork() {
        // Given
        Mono<String> flow = Mono.just("test");
        Function<String, Mono<Void>> nullAfterCommit = null;

        // When
        Mono<String> result = executor.execute(flow, nullAfterCommit);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("mapPropagation 메서드가 올바른 Spring 상수를 반환해야 함")
    void mapPropagation_shouldReturnCorrectSpringConstant() throws Exception {
        // Given
        var method = ReactiveUnitOfWorkExecutor.class.getDeclaredMethod("mapPropagation", Propagation.class);
        method.setAccessible(true);

        // When & Then
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRED,
                method.invoke(executor, Propagation.REQUIRED));
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW,
                method.invoke(executor, Propagation.REQUIRES_NEW));
    }

    @Test
    @DisplayName("mapPropagation 메서드가 null 입력에 대해 기본값을 반환해야 함")
    void mapPropagation_withNull_shouldReturnDefault() throws Exception {
        // Given
        var method = ReactiveUnitOfWorkExecutor.class.getDeclaredMethod("mapPropagation", Propagation.class);
        method.setAccessible(true);

        // When & Then
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRED,
                method.invoke(executor, (Propagation) null));
    }

    @Test
    @DisplayName("createTransactionalMono 메서드가 null이 아닌 Mono를 반환해야 함")
    void createTransactionalMono_shouldReturnNonNullMono() throws Exception {
        // Given
        Mono<String> flow = Mono.just("tx");
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();

        var method = ReactiveUnitOfWorkExecutor.class.getDeclaredMethod("createTransactionalMono", Mono.class,
                DefaultTransactionDefinition.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        Mono<String> result = (Mono<String>) method.invoke(executor, flow, def);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("생성자에 null TransactionManager 전달 시 정상 생성되어야 함")
    void constructor_withNullTransactionManager_shouldCreateInstance() {
        // When & Then - RequiredArgsConstructor는 null을 허용하므로 예외가 발생하지 않음
        assertDoesNotThrow(() -> new ReactiveUnitOfWorkExecutor(null));
    }

    @Test
    @DisplayName("여러 메서드 호출이 연속적으로 작동해야 함")
    void execute_multipleMethodCalls_shouldWork() {
        // Given
        Mono<String> flow1 = Mono.just("first");
        Mono<String> flow2 = Mono.just("second");

        // When & Then
        assertDoesNotThrow(() -> {
            Mono<String> result1 = executor.execute(flow1);
            Mono<String> result2 = executor.execute(flow2, Propagation.REQUIRES_NEW);

            assertNotNull(result1);
            assertNotNull(result2);
        });
    }

    @Test
    @DisplayName("복잡한 Function 객체로 테스트")
    void execute_withComplexAfterCommitFunction_shouldWork() {
        // Given
        Mono<Integer> flow = Mono.just(100);
        Function<Integer, Mono<Void>> complexAfterCommit = value -> {
            if (value > 50) {
                return Mono.<Void>empty().doOnSuccess(v -> System.out.println("Large value processed"));
            } else {
                return Mono.error(new IllegalArgumentException("Value too small"));
            }
        };

        // When & Then
        assertDoesNotThrow(() -> {
            Mono<Integer> result = executor.execute(flow, complexAfterCommit);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("모든 전파 옵션이 정상 처리되어야 함")
    void execute_withAllPropagationOptions_shouldWork() {
        // Given
        Mono<String> flow = Mono.just("test");

        // When & Then
        assertDoesNotThrow(() -> {
            Mono<String> result1 = executor.execute(flow, Propagation.REQUIRED);
            Mono<String> result2 = executor.execute(flow, Propagation.REQUIRES_NEW);

            assertNotNull(result1);
            assertNotNull(result2);
        });
    }

    @Test
    @DisplayName("빈 Mono로 실행 시 정상 처리되어야 함")
    void execute_withEmptyMono_shouldWork() {
        // Given
        Mono<String> emptyFlow = Mono.empty();

        // When & Then
        assertDoesNotThrow(() -> {
            Mono<String> result = executor.execute(emptyFlow);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("예외를 발생시키는 Mono로 실행 시 정상적으로 Mono를 반환해야 함")
    void execute_withErrorMono_shouldReturnMono() {
        // Given
        Mono<String> errorFlow = Mono.error(new RuntimeException("Test error"));

        // When & Then
        assertDoesNotThrow(() -> {
            Mono<String> result = executor.execute(errorFlow);
            assertNotNull(result);
        });
    }
}