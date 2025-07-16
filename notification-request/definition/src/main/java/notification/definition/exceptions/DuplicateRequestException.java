package notification.definition.exceptions;

public class DuplicateRequestException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public DuplicateRequestException(String message) {
        super(message);
    }

    public DuplicateRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateRequestException(Throwable cause) {
        super(cause);
    }

}
