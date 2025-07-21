package notification.infrastructure.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import notification.definition.vo.JsonPayload;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonPayloadConverter 테스트")
class JsonPayloadConverterTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JsonPayloadConverter jsonPayloadConverter;

    @Nested
    @DisplayName("toJsonPayload 메서드 테스트")
    class ToJsonPayloadTest {

        @Test
        @DisplayName("정상적인 객체를 JsonPayload로 변환한다")
        void shouldConvertObjectToJsonPayload() throws Exception {
            // given
            TestObject testObject = new TestObject("test", 123);
            String expectedJson = "{\"name\":\"test\",\"value\":123}";
            when(objectMapper.writeValueAsString(testObject)).thenReturn(expectedJson);

            // when
            JsonPayload result = jsonPayloadConverter.toJsonPayload(testObject);

            // then
            assertThat(result).isNotNull();
            assertThat(result.value()).isEqualTo(expectedJson);
            verify(objectMapper).writeValueAsString(testObject);
        }

        @Test
        @DisplayName("null 객체 변환시 IllegalArgumentException을 던진다")
        void shouldThrowIllegalArgumentExceptionWhenObjectIsNull() {
            // when & then
            assertThatThrownBy(() -> jsonPayloadConverter.toJsonPayload(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Object to convert cannot be null");

            verifyNoInteractions(objectMapper);
        }

        @Test
        @DisplayName("JSON 변환 실패시 RuntimeException을 던진다")
        void shouldThrowRuntimeExceptionWhenConversionFails() throws Exception {
            // given
            TestObject testObject = new TestObject("test", 123);
            JsonProcessingException mockException = mock(JsonProcessingException.class);
            when(objectMapper.writeValueAsString(testObject)).thenThrow(mockException);

            // when & then
            assertThatThrownBy(() -> jsonPayloadConverter.toJsonPayload(testObject))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Object conversion to JSON failed")
                    .hasCause(mockException);

            verify(objectMapper).writeValueAsString(testObject);
        }
    }

    @Nested
    @DisplayName("toJson 메서드 테스트")
    class ToJsonTest {

        @Test
        @DisplayName("정상적인 객체를 JSON 문자열로 변환한다")
        void shouldConvertObjectToJsonString() throws Exception {
            // given
            TestObject testObject = new TestObject("test", 123);
            String expectedJson = "{\"name\":\"test\",\"value\":123}";
            when(objectMapper.writeValueAsString(testObject)).thenReturn(expectedJson);

            // when
            String result = jsonPayloadConverter.toJson(testObject);

            // then
            assertThat(result).isEqualTo(expectedJson);
            verify(objectMapper).writeValueAsString(testObject);
        }

        @Test
        @DisplayName("null 객체 변환시 IllegalArgumentException을 던진다")
        void shouldThrowIllegalArgumentExceptionWhenObjectIsNull() {
            // when & then
            assertThatThrownBy(() -> jsonPayloadConverter.toJson(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Object to convert cannot be null");

            verifyNoInteractions(objectMapper);
        }

        @Test
        @DisplayName("JSON 변환 실패시 RuntimeException을 던진다")
        void shouldThrowRuntimeExceptionWhenConversionFails() throws Exception {
            // given
            TestObject testObject = new TestObject("test", 123);
            JsonProcessingException mockException = mock(JsonProcessingException.class);
            when(objectMapper.writeValueAsString(testObject)).thenThrow(mockException);

            // when & then
            assertThatThrownBy(() -> jsonPayloadConverter.toJson(testObject))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Object conversion to JSON failed")
                    .hasCause(mockException);

            verify(objectMapper).writeValueAsString(testObject);
        }
    }

    @Nested
    @DisplayName("fromJsonPayload 메서드 테스트")
    class FromJsonPayloadTest {

        @Test
        @DisplayName("JsonPayload를 객체로 변환한다")
        void shouldConvertJsonPayloadToObject() throws Exception {
            // given
            String json = "{\"name\":\"test\",\"value\":123}";
            JsonPayload jsonPayload = JsonPayload.of(json);
            TestObject expectedObject = new TestObject("test", 123);
            when(objectMapper.readValue(json, TestObject.class)).thenReturn(expectedObject);

            // when
            TestObject result = jsonPayloadConverter.fromJsonPayload(jsonPayload, TestObject.class);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("test");
            assertThat(result.value()).isEqualTo(123);
            verify(objectMapper).readValue(json, TestObject.class);
        }

        @Test
        @DisplayName("null JsonPayload 변환시 IllegalArgumentException을 던진다")
        void shouldThrowIllegalArgumentExceptionWhenJsonPayloadIsNull() {
            // when & then
            assertThatThrownBy(() -> jsonPayloadConverter.fromJsonPayload(null, TestObject.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("JsonPayload to convert cannot be null or empty");

            verifyNoInteractions(objectMapper);
        }

        @Test
        @DisplayName("JSON 파싱 실패시 RuntimeException을 던진다")
        void shouldThrowRuntimeExceptionWhenParsingFails() throws Exception {
            // given
            String invalidJson = "invalid json";
            JsonPayload jsonPayload = JsonPayload.of(invalidJson);
            JsonProcessingException mockException = mock(JsonProcessingException.class);
            when(objectMapper.readValue(invalidJson, TestObject.class)).thenThrow(mockException);

            // when & then
            assertThatThrownBy(() -> jsonPayloadConverter.fromJsonPayload(jsonPayload, TestObject.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("JsonPayload conversion to object failed")
                    .hasCause(mockException);

            verify(objectMapper).readValue(invalidJson, TestObject.class);
        }
    }

    @Nested
    @DisplayName("fromJson 메서드 테스트")
    class FromJsonTest {

        @Test
        @DisplayName("JSON 문자열을 객체로 변환한다")
        void shouldConvertJsonStringToObject() throws Exception {
            // given
            String json = "{\"name\":\"test\",\"value\":123}";
            TestObject expectedObject = new TestObject("test", 123);
            when(objectMapper.readValue(json, TestObject.class)).thenReturn(expectedObject);

            // when
            TestObject result = jsonPayloadConverter.fromJson(json, TestObject.class);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("test");
            assertThat(result.value()).isEqualTo(123);
            verify(objectMapper).readValue(json, TestObject.class);
        }

        @Test
        @DisplayName("null JSON 문자열 변환시 IllegalArgumentException을 던진다")
        void shouldThrowIllegalArgumentExceptionWhenJsonIsNull() {
            // when & then
            assertThatThrownBy(() -> jsonPayloadConverter.fromJson(null, TestObject.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("JSON string to convert cannot be null or empty");

            verifyNoInteractions(objectMapper);
        }

        @Test
        @DisplayName("빈 JSON 문자열 변환시 IllegalArgumentException을 던진다")
        void shouldThrowIllegalArgumentExceptionWhenJsonIsEmpty() {
            // when & then
            assertThatThrownBy(() -> jsonPayloadConverter.fromJson("", TestObject.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("JSON string to convert cannot be null or empty");

            verifyNoInteractions(objectMapper);
        }

        @Test
        @DisplayName("JSON 파싱 실패시 RuntimeException을 던진다")
        void shouldThrowRuntimeExceptionWhenParsingFails() throws Exception {
            // given
            String invalidJson = "invalid json";
            JsonProcessingException mockException = mock(JsonProcessingException.class);
            when(objectMapper.readValue(invalidJson, TestObject.class)).thenThrow(mockException);

            // when & then
            assertThatThrownBy(() -> jsonPayloadConverter.fromJson(invalidJson, TestObject.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("JSON conversion to object failed")
                    .hasCause(mockException);

            verify(objectMapper).readValue(invalidJson, TestObject.class);
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTest {

        private ObjectMapper realObjectMapper;
        private JsonPayloadConverter realConverter;

        @BeforeEach
        void setUp() {
            realObjectMapper = new ObjectMapper();
            realConverter = new JsonPayloadConverter(realObjectMapper);
        }

        @Test
        @DisplayName("객체를 JsonPayload로 변환 후 다시 객체로 변환한다")
        void shouldConvertObjectToJsonPayloadAndBack() {
            // given
            TestObject original = new TestObject("integration test", 456);

            // when
            JsonPayload jsonPayload = realConverter.toJsonPayload(original);
            TestObject result = realConverter.fromJsonPayload(jsonPayload, TestObject.class);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(original.name());
            assertThat(result.value()).isEqualTo(original.value());
        }

        @Test
        @DisplayName("객체를 JSON 문자열로 변환 후 다시 객체로 변환한다")
        void shouldConvertObjectToJsonStringAndBack() {
            // given
            TestObject original = new TestObject("string test", 789);

            // when
            String json = realConverter.toJson(original);
            TestObject result = realConverter.fromJson(json, TestObject.class);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(original.name());
            assertThat(result.value()).isEqualTo(original.value());
        }

        @Test
        @DisplayName("복잡한 객체도 정상적으로 변환한다")
        void shouldHandleComplexObject() {
            // given
            ComplexTestObject original = new ComplexTestObject(
                    "complex",
                    new TestObject("nested", 100),
                    java.util.List.of("item1", "item2"));

            // when
            JsonPayload jsonPayload = realConverter.toJsonPayload(original);
            ComplexTestObject result = realConverter.fromJsonPayload(jsonPayload, ComplexTestObject.class);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(original.name());
            assertThat(result.nested().name()).isEqualTo(original.nested().name());
            assertThat(result.nested().value()).isEqualTo(original.nested().value());
            assertThat(result.items()).containsExactlyElementsOf(original.items());
        }
    }

    // 테스트용 클래스들
    public record TestObject(String name, int value) {
    }

    public record ComplexTestObject(
            String name,
            TestObject nested,
            java.util.List<String> items) {
    }
}
