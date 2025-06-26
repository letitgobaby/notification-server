package notification.application.common.port.outbound;

public interface ObjectConverterPort {

    /**
     * 주어진 객체를 문자열 표현으로 직렬화합니다.
     *
     * @param object 직렬화할 객체
     * @return 직렬화된 문자열을 담은 Mono
     */
    String serialize(Object object);

    /**
     * 주어진 문자열을 특정 클래스의 객체로 역직렬화합니다.
     *
     * @param data  역직렬화할 문자열
     * @param clazz 역직렬화할 객체의 클래스 타입
     * @return 역직렬화된 객체를 담은 Mono
     */
    <T> T deserialize(String data, Class<T> clazz);

}
