package notification.application.common.port.outbound;

import notification.definition.vo.JsonPayload;

public interface JsonPayloadConverterPort {

    JsonPayload toJsonPayload(Object object);

    String toJson(Object object);

    <T> T fromJsonPayload(JsonPayload payload, Class<T> clazz);

    <T> T fromJson(String json, Class<T> clazz);

}
