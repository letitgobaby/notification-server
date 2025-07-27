package notification.adapter.db.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import notification.adapter.db.NotificationRequestRecipientEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * NotificationRequestRecipient Entity Repository
 */
public interface R2dbcNotificationRequestRecipientRepository
        extends ReactiveCrudRepository<NotificationRequestRecipientEntity, String> {

    /**
     * 요청 ID로 수신자 조회
     */
    Flux<NotificationRequestRecipientEntity> findByRequestId(String requestId);

    /**
     * 요청 ID로 수신자 삭제
     */
    Mono<Void> deleteByRequestId(String requestId);

}
