package notification.application.common.factory;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import notification.application.common.exceptions.JsonPayloadConvertException;
import notification.domain.common.vo.JsonPayload;

@Component
public final class JsonPayloadFactory {

    private final ObjectMapper objectMapper;

    public JsonPayloadFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonPayload toJsonPayload(Object object) {
        if (object == null) {
            throw new JsonPayloadConvertException("Object to convert cannot be null");
        }

        String json = toJson(object);
        return JsonPayload.from(json);
    }

    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new JsonPayloadConvertException("Failed to convert object to JSON", e);
        }
    }

    public <T> T fromJsonPayload(JsonPayload payload, Class<T> clazz) {
        try {
            return objectMapper.readValue(payload.payload(), clazz);
        } catch (Exception e) {
            throw new JsonPayloadConvertException("Failed to convert JsonPayload to object", e);
        }
    }

}
