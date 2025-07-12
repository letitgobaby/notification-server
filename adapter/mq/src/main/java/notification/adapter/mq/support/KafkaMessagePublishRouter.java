package notification.adapter.mq.support;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import notification.adapter.mq.payload.NotificationMessagePayload;
import notification.domain.enums.NotificationType;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KafkaMessagePublishRouter {

    private final List<KafkaMessagePublishSupport<? extends NotificationMessagePayload>> publishers;

    public <T extends NotificationMessagePayload> Mono<Void> publish(T payload, NotificationType type) {
        KafkaMessagePublishSupport<T> publisher = findPublisher(type);
        return publisher.publish(payload);
    }

    @SuppressWarnings("unchecked")
    private <T extends NotificationMessagePayload> KafkaMessagePublishSupport<T> findPublisher(
            NotificationType type) {
        return (KafkaMessagePublishSupport<T>) publishers.stream()
                .filter(p -> p.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No publisher for type: " + type));
    }
}
