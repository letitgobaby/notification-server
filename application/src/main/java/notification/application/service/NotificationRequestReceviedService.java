package notification.application.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestResult;
import notification.application.notifiation.mapper.NotificationRequestCommandMapper;
import notification.application.notifiation.port.inbound.NotificationRequestReceviedUseCase;
import notification.application.outbox.port.outbound.RequestOutboxEventPublisherPort;
import notification.application.service.support.NotificationRequestWithOutboxSaver;
import notification.definition.annotations.Idempotent;
import notification.definition.annotations.UnitOfWork;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRequestReceviedService implements NotificationRequestReceviedUseCase {

    private final NotificationRequestWithOutboxSaver notificationRequestWithOutboxSaver;
    private final NotificationRequestCommandMapper notificationRequestMapper;
    private final RequestOutboxEventPublisherPort requestOutboxEventPublisher;

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
    @UnitOfWork
    @Idempotent(argKey = "idempotencyKey", operationType = "NOTIFICATION_REQUEST_RECEIVED")
    @Override
    public Mono<NotificationRequestResult> handle(NotificationRequestCommand command, String idempotencyKey) {
        log.info("Handling notification request: {}", command);

        return Mono.just(command)
                .flatMap(notificationRequestMapper::fromCommand)
                .flatMap(notificationRequestWithOutboxSaver::save) // 알림 요청과 아웃박스 메시지를 저장
                .doOnSuccess(requestOutboxEventPublisher::publish) // 아웃박스 메시지 이벤트 발행
                .map(entity -> NotificationRequestResult.success(entity.getAggregateId()))
                .onErrorResume(e -> {
                    log.error("Failed to handle notification request: {}", e.getMessage(), e);
                    return Mono.just(NotificationRequestResult.failure(e.getMessage()));
                });
    }

}