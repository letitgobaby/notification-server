package notification.definition.exceptions;

public class Network4xxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Network4xxException(String message) {
        super(message);
    }

    public Network4xxException(String message, Throwable cause) {
        super(message, cause);
    }

    public Network4xxException(Throwable cause) {
        super(cause);
    }

}
