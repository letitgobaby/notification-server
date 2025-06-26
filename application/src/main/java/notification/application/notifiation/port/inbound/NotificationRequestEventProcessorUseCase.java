package notification.application.notifiation.port.inbound;

import notification.application.notifiation.events.NotificationRequestReceivedEvent;
import reactor.core.publisher.Mono;

public interface NotificationRequestEventProcessorUseCase {
    /**
     * 알림 요청 이벤트를 처리합니다.
     * 
     * @param eventId 이벤트 ID
     * @return Mono<Void> 성공 시 빈 Mono 반환
     */
    Mono<Void> process(NotificationRequestReceivedEvent event);

}
