package notification.application.notifiation.port.outbound;

import java.util.function.Function;

import org.reactivestreams.Publisher;

public interface TransactionExecutorPort {

    <T, P extends Publisher<T>> P executeTransaction(Function<Void, P> operation, boolean readOnly);

    default <T, P extends Publisher<T>> P executeTransaction(Function<Void, P> operation) {
        return executeTransaction(operation, false);
    }

}
