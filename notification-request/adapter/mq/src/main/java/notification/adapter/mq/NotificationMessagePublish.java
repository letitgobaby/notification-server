package notification.adapter.mq;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.adapter.mq.mapper.NotificationMessagePayloadMapper;
import notification.adapter.mq.support.KafkaMessagePublishRouter;
import notification.application.notifiation.port.outbound.message.NotificationMessagePublishPort;
import notification.domain.NotificationMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessagePublish implements NotificationMessagePublishPort {

    private final NotificationMessagePayloadMapper payloadMapper;
    private final KafkaMessagePublishRouter publishRouter;

    @Override
    public Mono<Void> publish(NotificationMessage message) {
        log.info("Publishing NotificationMessage: {}", message);

        return payloadMapper.toPayload(message).flatMap(payload -> {
            return publishRouter.publish(payload, message.getNotificationType());
        });
    }

}