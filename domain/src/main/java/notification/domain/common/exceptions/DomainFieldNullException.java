package notification.domain.common.exceptions;

public class DomainFieldNullException extends RuntimeException {

    public DomainFieldNullException(String message, Throwable cause) {
        super(message, cause);
    }

    public DomainFieldNullException(String message) {
        super(message);
    }

    public DomainFieldNullException(Throwable cause) {
        super(cause);
    }

}
