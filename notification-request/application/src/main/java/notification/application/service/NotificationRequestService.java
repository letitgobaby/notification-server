package notification.application.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.port.outbound.UnitOfWorkExecutorPort;
import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestResult;
import notification.application.notifiation.mapper.NotificationRequestCommandMapper;
import notification.application.notifiation.port.inbound.ProcessNotificationRequestUseCase;
import notification.application.outbox.port.outbound.RequestOutboxEventPublisherPort;
import notification.application.service.infrastructure.saver.NotificationRequestWithOutboxSaver;
import notification.definition.annotations.Idempotent;
import notification.definition.vo.outbox.RequestOutbox;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRequestService implements ProcessNotificationRequestUseCase {

    private final NotificationRequestWithOutboxSaver notificationRequestWithOutboxSaver;
    private final RequestOutboxEventPublisherPort requestOutboxEventPublisher;
    private final NotificationRequestCommandMapper notificationRequestMapper;
    private final UnitOfWorkExecutorPort unitOfWorkExecutor;

    /**
     * 알림 요청을 처리하는 메서드입니다.
     * 이 메서드는 알림 요청을 생성하고, 이를 데이터베이스에 저장한 후, 아웃박스 메시지를 생성하여 저장합니다.
     * 또한, 알림 요청이 즉시 처리되는 경우에는 `NotificationRequestReceivedEvent` 이벤트를 발행합니다.
     * 알림 요청이 스케줄링된 경우에는 이벤트를 발행하지 않습니다. -> 이벤트 발행은 outbox 스케줄링으로 처리합니다.
     *
     * @param command        알림 요청 커맨드
     * @param idempotencyKey 중복 요청 방지를 위한 키
     * @return Mono<NotificationRequestResult>
     */
    @Override
    @Idempotent(argKey = "idempotencyKey", operationType = "NOTIFICATION_REQUEST")
    public Mono<NotificationRequestResult> handle(NotificationRequestCommand command, String idempotencyKey) {
        log.info("Handling notification request [{}]: {}", idempotencyKey, command.requester());

        return unitOfWork(command)
                .map(result -> NotificationRequestResult.success(result.getAggregateId()))
                .onErrorResume(e -> {
                    log.error("Failed to handle notification request: {}", e.getMessage(), e);
                    return Mono.just(NotificationRequestResult.failure(e.getMessage()));
                });
    }

    /**
     * 알림 요청 커맨드를 처리하기 위한 단위 작업을 실행합니다.
     * 이 메서드는 트랜잭션 아웃박스 플로우를 실행하고, 결과를 아웃박스 이벤트 퍼블리셔에 전달합니다.
     *
     * @param command 알림 요청 커맨드
     * @return Mono<RequestOutbox>
     */
    private Mono<RequestOutbox> unitOfWork(NotificationRequestCommand command) {
        return unitOfWorkExecutor.execute(
                doTransactionalOutboxFlow(command),
                requestOutboxEventPublisher::publish // After-Commit
        );
    }

    /**
     * 알림 요청을 처리하는 트랜잭션 아웃박스 플로우를 수행합니다.
     * 이 메서드는 알림 요청 커맨드를 받아서, 이를 데이터베이스에 저장하고 아웃박스 메시지를 생성합니다.
     *
     * @param command 알림 요청 커맨드
     * @return Mono<RequestOutbox>
     */
    private Mono<RequestOutbox> doTransactionalOutboxFlow(NotificationRequestCommand command) {
        return Mono.just(command)
                .flatMap(notificationRequestMapper::fromCommand)
                .flatMap(notificationRequestWithOutboxSaver::save)
                .doOnError(e -> log.error("Failed to save notification request: {}", e.getMessage(), e));
    }

}
