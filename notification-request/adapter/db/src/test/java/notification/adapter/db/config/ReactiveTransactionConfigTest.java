package notification.adapter.db.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import io.r2dbc.spi.ConnectionFactory;

class ReactiveTransactionConfigTest {

    @Test
    void r2dbcTransactionManager_shouldReturnTransactionManager() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        ReactiveTransactionConfig config = new ReactiveTransactionConfig();

        R2dbcTransactionManager manager = config.r2dbcTransactionManager(connectionFactory);

        assertNotNull(manager);
        assertEquals(connectionFactory, manager.getConnectionFactory());
    }

    @Test
    void transactionalOperator_shouldReturnTransactionalOperator() {
        R2dbcTransactionManager transactionManager = mock(R2dbcTransactionManager.class);
        ReactiveTransactionConfig config = new ReactiveTransactionConfig();

        TransactionalOperator operator = config.transactionalOperator(transactionManager);

        assertNotNull(operator);
    }
}