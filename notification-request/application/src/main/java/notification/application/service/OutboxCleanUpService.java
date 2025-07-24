package notification.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.outbox.port.inbound.OutboxCleanUpUseCase;
import notification.application.outbox.port.outbound.MessageOutboxRepositoryPort;
import notification.application.outbox.port.outbound.RequestOutboxRepositoryPort;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxCleanUpService implements OutboxCleanUpUseCase {

    private final RequestOutboxRepositoryPort requestOutboxRepository;
    private final MessageOutboxRepositoryPort messageOutboxRepository;

    /**
     * 지정된 시간 이전에 생성된 In-Progress 상태의 Outbox 메시지를 정리합니다.
     * 이 메서드는 In-Progress 상태의 메시지를 정리하여 시스템의 안정성을 유지합니다.
     *
     * @param before 정리할 메시지의 기준 시간
     * @return Mono<Void> 완료를 나타내는 Mono
     */
    @Override
    public Mono<Void> cleanUpInProgressOutboxs(Instant before) {
        requestOutboxRepository.cleanUpInProgressOutboxs(before).subscribe();
        messageOutboxRepository.cleanUpInProgressOutboxs(before).subscribe();

        return Mono.empty();
    }

}
