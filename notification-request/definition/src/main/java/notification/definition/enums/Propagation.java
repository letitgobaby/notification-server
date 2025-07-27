package notification.definition.enums;

public enum Propagation {
    /**
     * Always start a new transaction.
     */
    REQUIRES_NEW,

    /**
     * Use the current transaction, or create a new one if none exists.
     */
    REQUIRED;

}
