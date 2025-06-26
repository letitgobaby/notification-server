package notification.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableTransactionManagement
public class ReactiveTransactionConfig {

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
