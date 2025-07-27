package notification.application.idempotency;

public class IdempotencyDuplicateKeyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdempotencyDuplicateKeyException(String message) {
        super(message);
    }

    public IdempotencyDuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdempotencyDuplicateKeyException(Throwable cause) {
        super(cause);
    }

}
