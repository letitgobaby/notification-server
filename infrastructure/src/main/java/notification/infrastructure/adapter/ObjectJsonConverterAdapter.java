package notification.infrastructure.adapter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.port.outbound.ObjectConverterPort;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectJsonConverterAdapter implements ObjectConverterPort {

    private final ObjectMapper objectMapper;

    @Override
    public String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public <T> T deserialize(String data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (Exception e) {
            log.error("Failed to deserialize JSON to object", e);
            throw new RuntimeException("Deserialization failed", e);
        }
    }

}
