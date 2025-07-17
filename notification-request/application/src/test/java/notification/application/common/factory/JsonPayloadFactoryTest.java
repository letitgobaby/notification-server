package notification.application.common.factory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import notification.application.common.JsonPayloadFactory;
import notification.application.common.port.outbound.ObjectConverterPort;
import notification.definition.exceptions.ObjectConversionException;
import notification.definition.vo.JsonPayload;

@DisplayName("JsonPayloadFactory 테스트")
class JsonPayloadFactoryTest {

    @Mock
    private ObjectConverterPort objectConverter;

    private JsonPayloadFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        factory = new JsonPayloadFactory(objectConverter);
    }

    public record TestData(String name, int age) {
    }

    @Nested
    @DisplayName("toJsonPayload 메서드")
    class ToJsonPayloadTest {

        @Test
        @DisplayName("정상적인 객체를 JsonPayload로 변환해야 한다")
        void shouldConvertObjectToJsonPayload() {
            // given
            TestData testData = new TestData("홍길동", 30);
            String expectedJson = "{\"name\":\"홍길동\",\"age\":30}";
            when(objectConverter.serialize(testData)).thenReturn(expectedJson);

            // when
            JsonPayload result = factory.toJsonPayload(testData);

            // then
            assertNotNull(result);
            assertEquals(expectedJson, result.value());
            verify(objectConverter).serialize(testData);
        }

        @Test
        @DisplayName("null 객체일 때 ObjectConversionException이 발생해야 한다")
        void shouldThrowExceptionWhenObjectIsNull() {
            // when & then
            ObjectConversionException exception = assertThrows(
                    ObjectConversionException.class,
                    () -> factory.toJsonPayload(null));
            assertEquals("Object to convert cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("직렬화 실패 시 ObjectConversionException이 발생해야 한다")
        void shouldThrowExceptionWhenSerializationFails() {
            // given
            TestData testData = new TestData("홍길동", 30);
            RuntimeException originalException = new RuntimeException("Serialization failed");
            when(objectConverter.serialize(testData)).thenThrow(originalException);

            // when & then
            ObjectConversionException exception = assertThrows(
                    ObjectConversionException.class,
                    () -> factory.toJsonPayload(testData));
            assertEquals("Failed to convert object to JSON", exception.getMessage());
            assertEquals(originalException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("toJson 메서드")
    class ToJsonTest {

        @Test
        @DisplayName("정상적인 객체를 JSON 문자열로 변환해야 한다")
        void shouldConvertObjectToJsonString() {
            // given
            TestData testData = new TestData("홍길동", 30);
            String expectedJson = "{\"name\":\"홍길동\",\"age\":30}";
            when(objectConverter.serialize(testData)).thenReturn(expectedJson);

            // when
            String result = factory.toJson(testData);

            // then
            assertEquals(expectedJson, result);
            verify(objectConverter).serialize(testData);
        }

        @Test
        @DisplayName("null 객체일 때 ObjectConversionException이 발생해야 한다")
        void shouldThrowExceptionWhenObjectIsNull() {
            // when & then
            ObjectConversionException exception = assertThrows(
                    ObjectConversionException.class,
                    () -> factory.toJson(null));
            assertEquals("Object to convert cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("직렬화 실패 시 ObjectConversionException이 발생해야 한다")
        void shouldThrowExceptionWhenSerializationFails() {
            // given
            TestData testData = new TestData("홍길동", 30);
            RuntimeException originalException = new RuntimeException("Serialization failed");
            when(objectConverter.serialize(testData)).thenThrow(originalException);

            // when & then
            ObjectConversionException exception = assertThrows(
                    ObjectConversionException.class,
                    () -> factory.toJson(testData));
            assertEquals("Failed to convert object to JSON", exception.getMessage());
            assertEquals(originalException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("fromJsonPayload 메서드")
    class FromJsonPayloadTest {

        @Test
        @DisplayName("정상적인 JsonPayload를 객체로 변환해야 한다")
        void shouldConvertJsonPayloadToObject() {
            // given
            String json = "{\"name\":\"홍길동\",\"age\":30}";
            JsonPayload payload = JsonPayload.of(json);
            TestData expectedData = new TestData("홍길동", 30);
            when(objectConverter.deserialize(json, TestData.class)).thenReturn(expectedData);

            // when
            TestData result = factory.fromJsonPayload(payload, TestData.class);

            // then
            assertNotNull(result);
            assertEquals("홍길동", result.name());
            assertEquals(30, result.age());
            verify(objectConverter).deserialize(json, TestData.class);
        }

        @Test
        @DisplayName("null JsonPayload일 때 ObjectConversionException이 발생해야 한다")
        void shouldThrowExceptionWhenPayloadIsNull() {
            // when & then
            ObjectConversionException exception = assertThrows(
                    ObjectConversionException.class,
                    () -> factory.fromJsonPayload(null, TestData.class));
            assertEquals("JsonPayload to convert cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("역직렬화 실패 시 ObjectConversionException이 발생해야 한다")
        void shouldThrowExceptionWhenDeserializationFails() {
            // given
            String invalidJson = "invalid json";
            JsonPayload payload = JsonPayload.of(invalidJson);
            RuntimeException originalException = new RuntimeException("Deserialization failed");
            when(objectConverter.deserialize(invalidJson, TestData.class)).thenThrow(originalException);

            // when & then
            ObjectConversionException exception = assertThrows(
                    ObjectConversionException.class,
                    () -> factory.fromJsonPayload(payload, TestData.class));
            assertEquals("Failed to convert JsonPayload to object", exception.getMessage());
            assertEquals(originalException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("fromJson 메서드")
    class FromJsonTest {

        @Test
        @DisplayName("정상적인 JSON 문자열을 객체로 변환해야 한다")
        void shouldConvertJsonStringToObject() {
            // given
            String json = "{\"name\":\"홍길동\",\"age\":30}";
            TestData expectedData = new TestData("홍길동", 30);
            when(objectConverter.deserialize(json, TestData.class)).thenReturn(expectedData);

            // when
            TestData result = factory.fromJson(json, TestData.class);

            // then
            assertNotNull(result);
            assertEquals("홍길동", result.name());
            assertEquals(30, result.age());
            verify(objectConverter).deserialize(json, TestData.class);
        }

        @Test
        @DisplayName("null JSON 문자열일 때 ObjectConversionException이 발생해야 한다")
        void shouldThrowExceptionWhenJsonIsNull() {
            // when & then
            ObjectConversionException exception = assertThrows(
                    ObjectConversionException.class,
                    () -> factory.fromJson(null, TestData.class));
            assertEquals("JSON string to convert cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("빈 JSON 문자열일 때 ObjectConversionException이 발생해야 한다")
        void shouldThrowExceptionWhenJsonIsEmpty() {
            // when & then
            ObjectConversionException exception = assertThrows(
                    ObjectConversionException.class,
                    () -> factory.fromJson("", TestData.class));
            assertEquals("JSON string to convert cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("역직렬화 실패 시 ObjectConversionException이 발생해야 한다")
        void shouldThrowExceptionWhenDeserializationFails() {
            // given
            String invalidJson = "invalid json";
            RuntimeException originalException = new RuntimeException("Deserialization failed");
            when(objectConverter.deserialize(invalidJson, TestData.class)).thenThrow(originalException);

            // when & then
            ObjectConversionException exception = assertThrows(
                    ObjectConversionException.class,
                    () -> factory.fromJson(invalidJson, TestData.class));
            assertEquals("Failed to convert JSON to object", exception.getMessage());
            assertEquals(originalException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("객체 -> JsonPayload -> 객체 변환이 정상 동작해야 한다")
        void shouldConvertObjectToJsonPayloadAndBack() {
            // given
            TestData originalData = new TestData("홍길동", 30);
            String json = "{\"name\":\"홍길동\",\"age\":30}";
            when(objectConverter.serialize(originalData)).thenReturn(json);
            when(objectConverter.deserialize(json, TestData.class)).thenReturn(originalData);

            // when
            JsonPayload payload = factory.toJsonPayload(originalData);
            TestData result = factory.fromJsonPayload(payload, TestData.class);

            // then
            assertNotNull(result);
            assertEquals(originalData.name(), result.name());
            assertEquals(originalData.age(), result.age());
            verify(objectConverter).serialize(originalData);
            verify(objectConverter).deserialize(json, TestData.class);
        }

        @Test
        @DisplayName("객체 -> JSON 문자열 -> 객체 변환이 정상 동작해야 한다")
        void shouldConvertObjectToJsonStringAndBack() {
            // given
            TestData originalData = new TestData("홍길동", 30);
            String json = "{\"name\":\"홍길동\",\"age\":30}";
            when(objectConverter.serialize(originalData)).thenReturn(json);
            when(objectConverter.deserialize(json, TestData.class)).thenReturn(originalData);

            // when
            String jsonString = factory.toJson(originalData);
            TestData result = factory.fromJson(jsonString, TestData.class);

            // then
            assertNotNull(result);
            assertEquals(originalData.name(), result.name());
            assertEquals(originalData.age(), result.age());
            verify(objectConverter).serialize(originalData);
            verify(objectConverter).deserialize(json, TestData.class);
        }
    }
}