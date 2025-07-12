package notification.adapter.mq.support;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.adapter.mq.payload.PushMessagePayload;
import notification.definition.exceptions.ObjectConversionException;
import notification.domain.enums.NotificationType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Slf4j
@Component
@RequiredArgsConstructor
public final class KafkaPushMessagePublish implements KafkaMessagePublishSupport<PushMessagePayload> {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.notification}")
    private String notificationTopic;

    @Override
    public NotificationType getType() {
        return NotificationType.PUSH;
    }

    @Override
    public Mono<Void> publish(PushMessagePayload payload) {
        String topic = notificationTopic + "." + getType().name().toLowerCase();
        String messageId = payload.getMessageId();

        return serializeMessage(payload)
                .map(messageJson -> new ProducerRecord<>(topic, messageId, messageJson))
                .map(producerRecord -> SenderRecord.create(producerRecord, messageId))
                .flatMapMany(senderRecord -> kafkaSender.send(Flux.just(senderRecord)))
                .flatMap(result -> {
                    if (result.exception() != null) {
                        return Mono.error(result.exception());
                    }
                    return Mono.just(result);
                })
                .doOnError(e -> {
                    log.error("Failed to publish message to Kafka topic '{}' with key '{}': {}",
                            topic, messageId, e.getMessage(), e);
                })
                .then();
    }

    private Mono<String> serializeMessage(PushMessagePayload payload) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(payload))
                .onErrorMap(JsonProcessingException.class, e -> {
                    return new ObjectConversionException(e.getMessage());
                });
    }

}
