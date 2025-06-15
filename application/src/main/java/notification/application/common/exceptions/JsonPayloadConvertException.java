package notification.application.common.exceptions;

public class JsonPayloadConvertException extends RuntimeException {

    public JsonPayloadConvertException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonPayloadConvertException(String message) {
        super(message);
    }

    public JsonPayloadConvertException(Throwable cause) {
        super(cause);
    }

}
