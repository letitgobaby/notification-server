package notification.application.service.processing.processor;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.port.outbound.UnitOfWorkExecutorPort;
import notification.application.notifiation.port.inbound.NotificationRequestEventProcessorUseCase;
import notification.application.service.infrastructure.loader.NotificationRequestOutboxLoader;
import notification.application.service.processing.handler.NotificationRequestExecutionHandler;
import notification.application.service.processing.handler.NotificationRequestOutboxHandler;
import notification.application.service.processing.handler.NotificationRequestProcessingHandler;
import notification.definition.annotations.UnitOfWork;
import notification.definition.vo.outbox.RequestOutbox;
import notification.domain.NotificationRequest;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRequestEventProcessor implements NotificationRequestEventProcessorUseCase {

    private final NotificationRequestOutboxLoader notificationRequestOutboxLoader;
    private final NotificationRequestProcessingHandler notificationRequestProcessingHandler;
    private final NotificationRequestOutboxHandler notificationRequestOutboxHandler;
    private final NotificationRequestExecutionHandler executionHandler;

    private final UnitOfWorkExecutorPort unitOfWorkExecutor;

    /**
     * 알림 요청 이벤트를 처리합니다. Outbox 메시지를 로드하고, 알림 요청을 처리한 후
     * 아웃박스 메시지를 삭제하고 알림 요청 상태를 업데이트합니다.
     * 
     * @param outbox RequestOutbox 알림 요청 아웃박스 메시지
     * @return 생성된 NotificationMessage 리스트
     */
    @UnitOfWork
    @Override
    public Mono<Void> process(RequestOutbox outbox) {
        log.info("Processing NotificationRequest with outbox: {}", outbox.getAggregateId());

        Mono<NotificationRequest> logic = notificationRequestOutboxLoader.load(outbox).flatMap(domain -> {
            return notificationRequestProcessingHandler.handle(domain)
                    .thenReturn(domain)
                    .doOnSuccess(v -> log.info("Successfully processed request: {}", outbox.getAggregateId()))
                    .onErrorResume(e -> executionHandler.handle(domain, outbox, e).thenReturn(domain));
        });

        return unitOfWorkExecutor.execute(
                logic, // 트랜잭션 내에서 실행되는 로직
                notificationRequestOutboxHandler::handle // After Commit: Outbox 메시지 처리
        ).then();
    }

}