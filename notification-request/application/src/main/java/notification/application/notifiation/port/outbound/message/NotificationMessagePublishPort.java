package notification.application.notifiation.port.outbound.message;

import notification.domain.NotificationMessage;
import reactor.core.publisher.Mono;

public interface NotificationMessagePublishPort {

    /**
     * 알림 메시지를 발행합니다.
     *
     * @param message 알림 메시지
     * @return Mono<Void>
     */
    Mono<Void> publish(NotificationMessage message);

}
