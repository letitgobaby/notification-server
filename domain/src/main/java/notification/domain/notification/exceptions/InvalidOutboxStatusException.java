package notification.domain.notification.exceptions;

public class InvalidOutboxStatusException extends RuntimeException {

    public InvalidOutboxStatusException(String message) {
        super(message);
    }

    public InvalidOutboxStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidOutboxStatusException(Throwable cause) {
        super(cause);
    }

}
