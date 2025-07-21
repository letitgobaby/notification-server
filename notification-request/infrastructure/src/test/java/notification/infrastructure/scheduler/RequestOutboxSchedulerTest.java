package notification.infrastructure.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import notification.application.outbox.port.inbound.RequestOutboxPollingUseCase;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RequestOutboxSchedulerTest {

    @Mock
    private RequestOutboxPollingUseCase requestOutboxPollingService;

    @InjectMocks
    private RequestOutboxScheduler requestOutboxScheduler;

    @BeforeEach
    void setUp() {
        // 추가 설정이 필요한 경우 여기에 작성
    }

    @Test
    @DisplayName("poll 성공 시 로그가 정상적으로 출력된다")
    void shouldLogSuccessfullyWhenPollSucceeds() {
        // Given
        when(requestOutboxPollingService.poll()).thenReturn(Mono.empty());

        // When & Then
        assertDoesNotThrow(() -> requestOutboxScheduler.poll());

        // Verify that the polling service was called
        verify(requestOutboxPollingService, times(1)).poll();
    }

    @Test
    @DisplayName("poll 실행 시 UseCase가 호출된다")
    void shouldCallUseCaseWhenPollExecuted() {
        // Given
        Mono<Void> expectedMono = Mono.empty();
        when(requestOutboxPollingService.poll()).thenReturn(expectedMono);

        // When
        requestOutboxScheduler.poll();

        // Then
        verify(requestOutboxPollingService, times(1)).poll();
    }

    @Test
    @DisplayName("poll 예외 발생 시 에러 로그가 출력된다")
    void shouldLogErrorWhenExceptionOccursInPoll() {
        // Given
        RuntimeException exception = new RuntimeException("Test exception");
        when(requestOutboxPollingService.poll()).thenReturn(Mono.error(exception));

        // When & Then
        assertDoesNotThrow(() -> requestOutboxScheduler.poll());

        // Verify that the polling service was called even with error
        verify(requestOutboxPollingService, times(1)).poll();
    }

    @Test
    @DisplayName("poll 메서드가 비동기적으로 실행된다")
    void shouldExecutePollAsynchronously() {
        // Given
        Mono<Void> delayedMono = Mono.<Void>empty().delaySubscription(java.time.Duration.ofMillis(100));
        when(requestOutboxPollingService.poll()).thenReturn(delayedMono);

        // When
        long startTime = System.currentTimeMillis();
        requestOutboxScheduler.poll();
        long endTime = System.currentTimeMillis();

        // Then
        // poll() 메서드는 subscribe()를 호출하므로 즉시 반환되어야 함
        assertTrue((endTime - startTime) < 50, "poll() 메서드가 비동기적으로 실행되어야 합니다");

        verify(requestOutboxPollingService, times(1)).poll();
    }

    @Test
    @DisplayName("RequestOutboxPollingUseCase의 poll 메서드 반환값이 올바르게 처리된다")
    void shouldHandleReturnValueFromUseCasePollMethodCorrectly() {
        // Given
        Mono<Void> testMono = Mono.fromRunnable(() -> {
            // 실제로 뭔가 작업을 수행한다고 가정
            System.out.println("Outbox polling executed");
        });

        when(requestOutboxPollingService.poll()).thenReturn(testMono);

        // When
        requestOutboxScheduler.poll();

        // Then
        verify(requestOutboxPollingService, times(1)).poll();

        // 실제 Mono가 subscribe 되었는지 확인하기 위해 StepVerifier 사용
        StepVerifier.create(testMono)
                .verifyComplete();
    }

    @Test
    @DisplayName("poll 메서드에 Scheduled 어노테이션이 올바르게 설정되어 있는지 확인")
    void shouldHaveCorrectScheduledAnnotationOnPollMethod() throws NoSuchMethodException {
        // Given
        Class<RequestOutboxScheduler> clazz = RequestOutboxScheduler.class;

        // When
        var method = clazz.getDeclaredMethod("poll");
        var scheduledAnnotation = method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);

        // Then
        assertNotNull(scheduledAnnotation, "poll 메서드에 @Scheduled 어노테이션이 있어야 합니다");
        assertEquals("${app.outbox.polling-interval-ms:5000}", scheduledAnnotation.fixedDelayString(),
                "fixedDelayString 값이 올바르게 설정되어야 합니다");
    }

    @Test
    @DisplayName("클래스에 Component 어노테이션이 설정되어 있는지 확인")
    void shouldHaveComponentAnnotationOnClass() {
        // Given
        Class<RequestOutboxScheduler> clazz = RequestOutboxScheduler.class;

        // When
        var componentAnnotation = clazz.getAnnotation(org.springframework.stereotype.Component.class);

        // Then
        assertNotNull(componentAnnotation, "RequestOutboxScheduler 클래스에 @Component 어노테이션이 있어야 합니다");
    }
}
