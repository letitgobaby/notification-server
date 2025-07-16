package notification.definition.exceptions;

public class ObjectConversionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ObjectConversionException(String message) {
        super(message);
    }

    public ObjectConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectConversionException(Throwable cause) {
        super(cause);
    }

}
