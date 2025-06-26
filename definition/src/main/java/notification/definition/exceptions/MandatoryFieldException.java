package notification.definition.exceptions;

public class MandatoryFieldException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public MandatoryFieldException(String message) {
        super(message);
    }

    public MandatoryFieldException(String message, Throwable cause) {
        super(message, cause);
    }

    public MandatoryFieldException(Throwable cause) {
        super(cause);
    }

}
