package notification.application.notifiation.port.inbound;

import notification.definition.vo.outbox.RequestOutbox;
import reactor.core.publisher.Mono;

public interface NotificationRequestEventProcessorUseCase {

    /**
     * 알림 요청 이벤트를 처리합니다.
     * 
     * @param outbox RequestOutbox 알림 요청 아웃박스 메시지
     * @return Mono<Void> 성공 시 빈 Mono 반환
     */
    Mono<Void> process(RequestOutbox outbox);

}
