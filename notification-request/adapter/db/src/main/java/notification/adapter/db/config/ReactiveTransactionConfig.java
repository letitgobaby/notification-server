package notification.adapter.db.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

import io.r2dbc.spi.ConnectionFactory;

@Configuration
@EnableR2dbcRepositories(basePackages = "notification.adapter.db.repository")
@EnableTransactionManagement
public class ReactiveTransactionConfig {

    /**
     * R2DBC 트랜잭션 매니저를 설정합니다.
     * 실제 DB 연결은 Adapter에서 설정되어야 합니다.
     *
     * @return R2dbcTransactionManager
     */
    @Bean
    public R2dbcTransactionManager r2dbcTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    /**
     * R2DBC 트랜잭션 매니저를 사용하여 트랜잭션을 관리합니다.
     * 실제 DB 연결은 Adapter에서 설정되어야 합니다.
     *
     * @param transactionManager
     * @return
     */
    @Bean
    public TransactionalOperator transactionalOperator(R2dbcTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }

}
