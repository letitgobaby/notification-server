package notification.adapter.mq.support;

import notification.adapter.mq.payload.NotificationMessagePayload;
import notification.domain.enums.NotificationType;
import reactor.core.publisher.Mono;

public sealed interface KafkaMessagePublishSupport<T extends NotificationMessagePayload>
        permits KafkaSmsMessagePublish, KafkaPushMessagePublish, KafkaEmailMessagePublish {

    NotificationType getType(); // EMAIL, SMS, PUSH

    Mono<Void> publish(T payload);

}
