package notification.definition.exceptions;

public class Network5xxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Network5xxException(String message) {
        super(message);
    }

    public Network5xxException(String message, Throwable cause) {
        super(message, cause);
    }

    public Network5xxException(Throwable cause) {
        super(cause);
    }

}
