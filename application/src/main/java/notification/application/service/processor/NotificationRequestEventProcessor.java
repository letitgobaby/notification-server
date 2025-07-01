package notification.application.service.processor;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.inbound.NotificationRequestEventProcessorUseCase;
import notification.application.service.support.NotificationRequestExecutionHandler;
import notification.application.service.support.NotificationRequestOutboxLoader;
import notification.application.service.support.NotificationRequestProcessingHandler;
import notification.definition.annotations.UnitOfWork;
import notification.definition.vo.outbox.RequestOutbox;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRequestEventProcessor implements NotificationRequestEventProcessorUseCase {

    private final NotificationRequestOutboxLoader notificationRequestOutboxLoader;
    private final NotificationRequestProcessingHandler notificationRequestProcessingHandler;
    private final NotificationRequestExecutionHandler executionHandler;

    /**
     * NotificationRequest를 기반으로 NotificationMessage 리스트를 생성합니다.
     * 
     * @param request 알림 요청 정보
     * @return 생성된 NotificationMessage 리스트
     */
    @UnitOfWork
    @Override
    public Mono<Void> process(RequestOutbox outbox) {
        return notificationRequestOutboxLoader.load(outbox).flatMap(domain -> {
            return notificationRequestProcessingHandler.handle(domain).then()
                    .onErrorResume(e -> executionHandler.handle(domain, outbox, e));
        });
    }

}
