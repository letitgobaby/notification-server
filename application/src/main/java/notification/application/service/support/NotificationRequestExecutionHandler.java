package notification.application.service.support;

import java.time.Instant;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.outbound.persistence.NotificationRequestRepositoryPort;
import notification.application.notifiation.port.outbound.persistence.RequestOutboxMessageRepositoryPort;
import notification.definition.exceptions.Network4xxException;
import notification.domain.NotificationRequest;
import notification.domain.RequestOutboxMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestExecutionHandler {

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long MAX_RETRY_DELAY = 1800; // 최대 재시도 간격 (30분)

    private final NotificationRequestRepositoryPort notificationRequestRepository;
    private final RequestOutboxMessageRepositoryPort requestOutboxMessageRepository;

    /**
     * 알림 요청 이벤트 처리 중 예외가 발생했을 때 호출되는 메서드입니다.
     * 
     * @param domain               알림 요청 도메인 객체
     * @param requestOutboxMessage Outbox 메시지
     * @param e                    발생한 예외
     * @return 처리 결과 Mono<Void>
     */
    public Mono<Void> handle(
            NotificationRequest domain, RequestOutboxMessage requestOutboxMessage, Throwable e) {
        log.error("Error processing notification request event: {}", e.getMessage(), e);

        if (e instanceof Network4xxException) {
            return handleNetworkClientException(domain, requestOutboxMessage, e);
        }

        // + NetworkServerException 발생 시 재시도 로직
        return handleRetryLater(domain, requestOutboxMessage, e);
    }

    /**
     * 재시도 간격을 두고 Outbox 메시지를 FAILED 상태로 업데이트합니다.
     * 
     * @param outbox Outbox 메시지
     * @param e      발생한 예외
     * @return Mono.empty() 빈 Mono 반환
     */
    private Mono<Void> handleRetryLater(
            NotificationRequest domain, RequestOutboxMessage outbox, Throwable e) {
        log.warn("Retrying later for outbox message: {}", outbox.getAggregateId(), e);

        if (outbox.isMaxRetryAttemptsReached(MAX_RETRY_ATTEMPTS)) {
            return handleMaxRetryExceeded(domain, outbox, e);
        }

        int retryAttempts = outbox.getRetryAttempts();
        long delaySeconds = (long) Math.pow(2, retryAttempts) * 30; // 30s, 60s, 120s, ...
        Instant nextRetryAt = Instant.now().plusSeconds(Math.min(delaySeconds, MAX_RETRY_DELAY));
        outbox.markAsFailed(nextRetryAt);

        return requestOutboxMessageRepository.save(outbox)
                .doOnError(err -> log.error("Failed to update outbox to FAILED: {}", err.getMessage(), err))
                .onErrorResume(err -> Mono.empty())
                .then(Mono.empty());
    }

    /**
     * 최대 재시도 횟수를 초과한 경우 Outbox 메시지를 FAILED 상태로 업데이트하고
     * NotificationRequest를 FAILED 상태로 변경합니다.
     * 
     * @param domain 알림 요청 도메인 객체
     * @param outbox Outbox 메시지
     * @param e      발생한 예외
     * @return Mono.empty() 빈 Mono 반환
     */
    private Mono<Void> handleMaxRetryExceeded(
            NotificationRequest domain, RequestOutboxMessage outbox, Throwable e) {
        log.error("Max retry attempts reached for request: {}", outbox.getAggregateId());

        domain.markAsFailed("Max retry attempts reached : " + MAX_RETRY_ATTEMPTS);

        return clearOutbox(outbox, domain);
    }

    /**
     * NetworkClientException이 발생한 경우 Outbox 메시지를 삭제하고
     * NotificationRequest를 FAILED 상태로 변경합니다.
     * 
     * @param domain 알림 요청 도메인 객체
     * @param outbox Outbox 메시지
     * @param e      발생한 예외
     * @return Mono.empty() 빈 Mono 반환
     */
    private Mono<Void> handleNetworkClientException(
            NotificationRequest domain, RequestOutboxMessage outbox, Throwable e) {
        log.error("NetworkClientException occurred for request: {}", outbox.getAggregateId(), e);

        domain.markAsFailed(e.getMessage());

        return clearOutbox(outbox, domain);
    }

    private Mono<Void> clearOutbox(RequestOutboxMessage outbox, NotificationRequest domain) {
        return requestOutboxMessageRepository.deleteById(outbox.getOutboxId())
                .then(notificationRequestRepository.update(domain))
                .doOnError(err -> log.error("Failed to clear outbox message: {}", err.getMessage(), err))
                .onErrorResume(err -> Mono.empty())
                .then(Mono.empty());
    }

}
