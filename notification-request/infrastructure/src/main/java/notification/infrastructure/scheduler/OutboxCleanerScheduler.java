package notification.infrastructure.scheduler;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.outbox.port.inbound.OutboxCleanUpUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanerScheduler {

    private final OutboxCleanUpUseCase outboxCleanUp;

    /**
     * 스케줄러를 통해 In-Progress 상태의 Outbox 메시지를 정리합니다.
     * In-Progress + instanceId가 할당된 Outbox 메시지를 정리하여 시스템의 안정성을 유지합니다.
     *
     */
    @Scheduled(fixedDelayString = "${app.outbox.cleanup-interval-ms:60000}") // 기본값 1분
    public void cleanUpInProgressOutboxs() {
        log.info("Starting Outbox cleanup for In-Progress messages...");

        Instant before = Instant.now().minusSeconds(60); // 1분 전의 시간 기준
        outboxCleanUp.cleanUpInProgressOutboxs(before)
                .doOnSuccess(v -> log.info("Successfully cleaned up in-progress outbox messages before: {}", before))
                .doOnError(e -> log.error("Error cleaning up in-progress outbox messages: {}", e.getMessage(), e))
                .subscribe();
    }

}
