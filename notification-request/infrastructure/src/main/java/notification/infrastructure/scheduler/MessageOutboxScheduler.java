package notification.infrastructure.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.outbox.port.inbound.MessageOutboxPollingUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageOutboxScheduler {

    private final MessageOutboxPollingUseCase messageOutboxPollingService;

    /**
     * Outbox 메시지를 주기적으로 폴링하여 처리합니다.
     *
     * @return Mono<Void>
     */
    @Scheduled(fixedDelayString = "${app.outbox.polling-interval-ms:5000}") // 기본값 5초 (5000ms)
    public void poll() {
        log.info("Starting MessageOutbox polling...");
        messageOutboxPollingService.poll()
                .doOnSuccess(unused -> log.info("MessageOutbox polling completed successfully."))
                .doOnError(e -> log.error("Error during MessageOutbox polling: {}", e.getMessage(), e))
                .subscribe(); // 비동기적으로 실행
    }

}
