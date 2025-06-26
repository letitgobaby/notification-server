package notification.definition.exceptions;

public class PolicyViolationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public PolicyViolationException(String message) {
        super(message);
    }

    public PolicyViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PolicyViolationException(Throwable cause) {
        super(cause);
    }

}
