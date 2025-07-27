package notification.infrastructure.adapter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.port.outbound.JsonPayloadConverterPort;
import notification.definition.vo.JsonPayload;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonPayloadConverter implements JsonPayloadConverterPort {

    private final ObjectMapper objectMapper;

    @Override
    public JsonPayload toJsonPayload(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Object to convert cannot be null");
        }

        try {
            String json = objectMapper.writeValueAsString(object);
            return JsonPayload.of(json);
        } catch (Exception e) {
            log.error("Failed to convert object to JSON", e);
            throw new RuntimeException("Object conversion to JSON failed", e);
        }
    }

    @Override
    public String toJson(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Object to convert cannot be null");
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Failed to convert object to JSON", e);
            throw new RuntimeException("Object conversion to JSON failed", e);
        }
    }

    @Override
    public <T> T fromJsonPayload(JsonPayload payload, Class<T> clazz) {
        if (payload == null || payload.value() == null || payload.value().isEmpty()) {
            throw new IllegalArgumentException("JsonPayload to convert cannot be null or empty");
        }

        try {
            return objectMapper.readValue(payload.value(), clazz);
        } catch (Exception e) {
            log.error("Failed to convert JsonPayload to object", e);
            throw new RuntimeException("JsonPayload conversion to object failed", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            throw new IllegalArgumentException("JSON string to convert cannot be null or empty");
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Failed to convert JSON to object", e);
            throw new RuntimeException("JSON conversion to object failed", e);
        }
    }

}
