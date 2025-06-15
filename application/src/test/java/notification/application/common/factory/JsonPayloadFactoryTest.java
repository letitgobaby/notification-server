package notification.application.common.factory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import notification.application.common.exceptions.JsonPayloadConvertException;
import notification.domain.common.vo.JsonPayload;

class JsonPayloadFactoryTest {

    private JsonPayloadFactory factory;

    @BeforeEach
    void setUp() {
        factory = new JsonPayloadFactory(new ObjectMapper());
    }

    public record Dummy(String name) {
    }

    @Test
    void toJson_shouldSerializeObject() {
        Dummy dummy = new Dummy("test");
        String json = factory.toJson(dummy);
        assertTrue(json.contains("\"name\":\"test\""));
    }

    @Test
    void toJsonPayload_shouldReturnJsonPayload() {
        Dummy dummy = new Dummy("test");
        JsonPayload payload = factory.toJsonPayload(dummy);
        assertNotNull(payload);
        assertTrue(payload.toString().contains("\"name\":\"test\""));
    }

    @Test
    void toJson_shouldThrowRuntimeExceptionOnError() {
        Object invalid = new Object();
        assertThrows(RuntimeException.class, () -> factory.toJson(invalid));
    }

    @Test
    void fromJsonPayload_shouldDeserializeToObject() {
        Dummy dummy = new Dummy("test");
        JsonPayload payload = factory.toJsonPayload(dummy);
        Dummy result = factory.fromJsonPayload(payload, Dummy.class);
        assertNotNull(result);
        assertEquals("test", result.name);
    }

    @Test
    void fromJsonPayload_shouldThrowJsonPayloadConvertExceptionOnError() {
        JsonPayload invalidPayload = new JsonPayload("invalid json");

        assertThrows(JsonPayloadConvertException.class, () -> {
            factory.fromJsonPayload(invalidPayload, Dummy.class);
        });
    }

}