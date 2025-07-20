package notification.application.notifiation.port.inbound;

import notification.definition.vo.outbox.MessageOutbox;
import reactor.core.publisher.Mono;

public interface NotificationMessageEventProcessorUseCase {

    /**
     * 알림 요청에 대한 스케줄링된 메시지를 처리합니다.
     * 
     * @param notificationRequestId 알림 요청 ID
     * @return 처리 결과
     */
    Mono<Void> process(MessageOutbox outbox);

}
