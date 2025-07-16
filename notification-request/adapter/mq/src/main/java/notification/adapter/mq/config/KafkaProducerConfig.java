package notification.adapter.mq.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private int retries;

    /**
     * Kafka SenderOptions 빈을 생성합니다.
     * 이 옵션은 Kafka Producer의 설정을 정의합니다.
     */
    @Bean
    public SenderOptions<String, String> kafkaSenderOptions() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);

        return SenderOptions.create(props);
    }

    /**
     * KafkaSender 빈을 생성합니다.
     * 이 빈은 메시지를 Kafka로 발행하는 데 사용됩니다.
     */
    @Bean
    public KafkaSender<String, String> kafkaNotificationSender(
            SenderOptions<String, String> kafkaSenderOptions) {
        return KafkaSender.create(kafkaSenderOptions);
    }

}
