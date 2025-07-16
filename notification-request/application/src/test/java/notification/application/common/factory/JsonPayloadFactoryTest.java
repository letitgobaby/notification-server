package notification.application.common.factory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import notification.application.common.JsonPayloadFactory;
import notification.application.common.port.outbound.ObjectConverterPort;
import notification.definition.exceptions.ObjectConversionException;
import notification.definition.vo.JsonPayload;

class JsonPayloadFactoryTest {

    private ObjectConverterPort objectConverter = mock(ObjectConverterPort.class);
    private JsonPayloadFactory factory;

    @BeforeEach
    void setUp() {
        factory = new JsonPayloadFactory(objectConverter);
    }

    public record Dummy(String name) {
    }

    @Test
    void toJson_shouldSerializeObject() {
        Dummy dummy = new Dummy("test");
        when(objectConverter.serialize(dummy)).thenReturn("{\"name\":\"test\"}");

        String json = factory.toJson(dummy);

        assertTrue(json.contains("\"name\":\"test\""));
    }

    @Test
    void toJsonPayload_shouldReturnJsonPayload() {
        Dummy dummy = new Dummy("test");
        when(objectConverter.serialize(dummy)).thenReturn("{\"name\":\"test\"}");

        JsonPayload payload = factory.toJsonPayload(dummy);

        assertNotNull(payload);
        assertTrue(payload.toString().contains("\"name\":\"test\""));
    }

    @Test
    void toJson_shouldThrowRuntimeExceptionOnError() {
        Object invalid = new Object();
        when(objectConverter.serialize(invalid)).thenThrow(new ObjectConversionException("Serialization error"));

        assertThrows(ObjectConversionException.class, () -> {
            factory.toJson(invalid);
        });
    }

    @Test
    void fromJsonPayload_shouldDeserializeToObject() {
        Dummy dummy = new Dummy("test");
        when(objectConverter.deserialize("{\"name\":\"test\"}", Dummy.class)).thenReturn(dummy);
        when(objectConverter.serialize(dummy)).thenReturn("{\"name\":\"test\"}");

        JsonPayload payload = factory.toJsonPayload(dummy);
        Dummy result = factory.fromJsonPayload(payload, Dummy.class);

        assertNotNull(result);
        assertEquals("test", result.name);
    }

    @Test
    void fromJsonPayload_shouldThrowJsonPayloadConvertExceptionOnError() {
        JsonPayload invalidPayload = new JsonPayload("invalid json");
        when(objectConverter.deserialize("invalid json", Dummy.class))
                .thenThrow(new ObjectConversionException("Deserialization error"));

        assertThrows(ObjectConversionException.class, () -> {
            factory.fromJsonPayload(invalidPayload, Dummy.class);
        });
    }

}