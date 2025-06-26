package notification.application.service.support;

import java.time.Instant;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.outbound.persistence.NotificationMessageRepositoryPort;
import notification.application.notifiation.port.outbound.persistence.OutboxMessageRepositoryPort;
import notification.definition.exceptions.Network4xxException;
import notification.domain.NotificationMessage;
import notification.domain.OutboxMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageExceptionHandler {

    private static final int MAX_RETRY_ATTEMPTS = 10; // 최대 재시도 횟수
    private static final long MAX_RETRY_DELAY = 18000; // 최대 재시도

    private final NotificationMessageRepositoryPort notificationMessageRepository;
    private final OutboxMessageRepositoryPort outboxMessageRepository;

    /**
     * 알림 메시지 처리 중 예외가 발생했을 때 호출되는 메서드입니다.
     * 
     * @param message 알림 메시지
     * @param outbox  아웃박스 메시지
     * @param e       발생한 예외
     * @return 처리 결과
     */
    public Mono<Void> handle(NotificationMessage message, OutboxMessage outbox, Throwable e) {
        log.error("Error processing message: {}", outbox.getAggregateId(), e);

        if (e instanceof Network4xxException) {
            log.warn("Client error occurred, marking message as FAILED: {}", outbox.getAggregateId());

            message.markAsFailed("Client error: " + e.getMessage());
            return handleCompletedMessage(message, outbox);
        }

        if (outbox.isMaxRetryAttemptsReached(MAX_RETRY_ATTEMPTS)) {
            log.error("Max retry attempts reached for message: {}", outbox.getAggregateId());

            message.markAsFailed("Max retry attempts reached: " + MAX_RETRY_ATTEMPTS);
            return handleCompletedMessage(message, outbox);
        }

        long delay = calculateRetryDelaySeconds(outbox.getRetryAttempts());
        Instant nextRetryAt = Instant.now().plusSeconds(Math.min(delay, MAX_RETRY_DELAY));

        outbox.markAsFailed(nextRetryAt);
        return outboxMessageRepository.update(outbox).then()
                .onErrorResume(err -> {
                    log.error("Failed to update outbox to FAILED: {}", err.getMessage(), err);
                    return Mono.empty();
                });
    }

    /**
     * 알림 메시지가 성공 or 실패 처리된 후, 알림 메시지 상태를 업데이트하고
     * 아웃박스 메시지를 삭제합니다.
     * 
     * @param message 알림 메시지
     * @param outbox  아웃박스 메시지
     * @return 처리 결과
     */
    private Mono<Void> handleCompletedMessage(NotificationMessage message, OutboxMessage outbox) {
        return Mono.zip(
                notificationMessageRepository.update(message),
                outboxMessageRepository.deleteById(outbox.getOutboxId())).then();
    }

    private long calculateRetryDelaySeconds(int attempts) {
        return (long) Math.pow(2, attempts) * 30; // exponential backoff: 30s, 60s, 120s...
    }

}
