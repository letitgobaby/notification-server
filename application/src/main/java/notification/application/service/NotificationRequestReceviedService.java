package notification.application.service;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestResult;
import notification.application.notifiation.events.NotificationRequestReceivedEvent;
import notification.application.notifiation.factory.NotificationRequestFactory;
import notification.application.notifiation.port.inbound.NotificationRequestReceviedUseCase;
import notification.application.service.support.NotificationRequestWithOutboxSaver;
import notification.definition.annotations.UnitOfWork;
import notification.domain.RequestOutboxMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRequestReceviedService implements NotificationRequestReceviedUseCase {

    private final NotificationRequestWithOutboxSaver notificationRequestWithOutboxSaver;
    private final NotificationRequestFactory notificationRequestFactory;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 1. 알림요청과 아웃박스 저장
     * 2. 알림요청 이벤트 발행 (after commit)
     * - 알림요청이 즉시 처리되는 경우에는 NotificationRequestReceivedEvent를 발행합니다.
     * - 알림요청이 스케줄링된 경우에는 이벤트를 발행하지 않습니다. -> 이벤트 발행은 outbox 스케줄링으로 처리합니다.
     * 
     * 
     * 3. NotificationRequestReceivedEvent 이벤트를 리슨
     * - 알림요청 Processing 상태 변경
     * - 취소 요청 확인해서 취소처리
     * 4. 알림 요청에 대한 알림 메세지 리스트 생성
     * 5. 개별 메세지와 아웃박스 저장
     * 6. 개별 메세지 이벤트 발행 (after commit)
     * - 알림요청이 즉시 처리되는 경우에는 NotificationMessageReadyEvent 발행합니다.
     * - 알림요청이 스케줄링된 경우에는 이벤트를 발행하지 않습니다. -> 이벤트 발행은 outbox 스케줄링으로 처리합니다.
     * 7. NotificationRequestReceivedEvent의 아웃박스 메세지 삭제
     * 
     * 
     * 7. NotificationMessageReadyEvent 이벤트를 리슨해서 MQ에 메세지 발행
     * 8. 알림요청의 상태를 Dispatched로 변경
     * 8. 큐에서 메세지 수신해서 실제 발송
     * - 알림 발송 성공. 알림요청 Dispachted 상태 변경
     * 9. NotificationMessageReadyEvent의 아웃박스 메세지 삭제
     * 
     * 
     * -- 아웃박스 리트라이 스케줄러 동작 (outbox 스케줄링으로 처리)
     * - status가 PENDING & FAILED인 OutboxMessage를 조회
     * - && nextRetryAt이 현재 시간보다 이전인 OutboxMessage를 조회
     * -- 2. outboxMessage를 조회해서 이벤트 객체로 파싱한 뒤 이벤트 발행
     */

    /**
     * 알림 요청을 처리하는 메서드입니다.
     * 이 메서드는 알림 요청을 생성하고, 이를 데이터베이스에 저장한 후, 아웃박스 메시지를 생성하여 저장합니다.
     * 또한, 알림 요청이 즉시 처리되는 경우에는 `NotificationRequestReceivedEvent` 이벤트를 발행합니다.
     * 알림 요청이 스케줄링된 경우에는 이벤트를 발행하지 않습니다. -> 이벤트 발행은 outbox 스케줄링으로 처리합니다.
     * 
     * @param command 알림 요청 커맨드
     * @return Mono<NotificationRequestResult>
     *         - 성공 시: 알림 요청 ID와 성공 메시지를 포함한 `NotificationRequestResult` 객체
     *         - 실패 시: 실패 메시지를 포함한 `NotificationRequestResult`
     */
    @UnitOfWork
    @Override
    public Mono<NotificationRequestResult> handle(NotificationRequestCommand command, String idempotencyKey) {
        log.info("Handling notification request: {}", command);

        return Mono.just(command)
                .map(notificationRequestFactory::fromCommand)
                .flatMap(notificationRequestWithOutboxSaver::save) // 알림 요청과 아웃박스 메시지를 저장
                .doOnSuccess(savedOutbox -> publishEventOutbox(savedOutbox, command.scheduledAt()))
                .map(entity -> NotificationRequestResult.success(entity.getAggregateId()))
                .onErrorResume(e -> {
                    log.error("Failed to handle notification request: {}", e.getMessage(), e);
                    return Mono.just(NotificationRequestResult.failure(e.getMessage()));
                });
    }

    /**
     * 알림 요청 이벤트를 발행합니다.
     * * 알림 요청이 즉시 처리되는 경우에는 NotificationRequestReceivedEvent를 발행합니다.
     * * 알림 요청이 스케줄링된 경우에는 이벤트를 발행하지 않습니다.
     * 
     * @param savedRequest
     */
    private void publishEventOutbox(RequestOutboxMessage savedOutbox, Instant scheduledAt) {
        savedOutbox.getNextRetryAt();

        Instant buffetTime = Instant.now().plusSeconds(5); // 버퍼 타임 설정 (5초)
        if (scheduledAt != null && scheduledAt.isAfter(buffetTime)) {
            log.info("Notification request is scheduled for future: {}", scheduledAt);
            return; // 스케줄링된 요청은 이벤트 발행하지 않음
        }

        // 즉시 처리되는 요청에 대해서만 이벤트 발행
        applicationEventPublisher.publishEvent(
                new NotificationRequestReceivedEvent(this, savedOutbox));
    }

}