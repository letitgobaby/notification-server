package notification.application.common;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import notification.application.common.port.outbound.ObjectConverterPort;
import notification.definition.exceptions.ObjectConversionException;
import notification.definition.vo.JsonPayload;

@Component
@RequiredArgsConstructor
public final class JsonPayloadFactory {

    private final ObjectConverterPort objectConverter;

    public JsonPayload toJsonPayload(Object object) {
        if (object == null) {
            throw new ObjectConversionException("Object to convert cannot be null");
        }

        String json = toJson(object);
        return JsonPayload.of(json);
    }

    public String toJson(Object object) {
        if (object == null) {
            throw new ObjectConversionException("Object to convert cannot be null");
        }

        try {
            return objectConverter.serialize(object);
        } catch (Exception e) {
            throw new ObjectConversionException("Failed to convert object to JSON", e);
        }
    }

    public <T> T fromJsonPayload(JsonPayload payload, Class<T> clazz) {
        if (payload == null || payload.value() == null || payload.value().isEmpty()) {
            throw new ObjectConversionException("JsonPayload to convert cannot be null or empty");
        }

        try {
            return objectConverter.deserialize(payload.value(), clazz);
        } catch (Exception e) {
            throw new ObjectConversionException("Failed to convert JsonPayload to object", e);
        }
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            throw new ObjectConversionException("JSON string to convert cannot be null or empty");
        }

        try {
            return objectConverter.deserialize(json, clazz);
        } catch (Exception e) {
            throw new ObjectConversionException("Failed to convert JSON to object", e);
        }
    }

}
