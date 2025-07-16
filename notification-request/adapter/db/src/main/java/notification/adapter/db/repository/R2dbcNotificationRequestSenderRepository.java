package notification.adapter.db.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import notification.adapter.db.NotificationRequestSenderEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * NotificationRequestSender Entity Repository
 */
public interface R2dbcNotificationRequestSenderRepository
        extends ReactiveCrudRepository<NotificationRequestSenderEntity, String> {

    /**
     * 요청 ID로 발신자 정보 조회
     */
    Flux<NotificationRequestSenderEntity> findByRequestId(String requestId);

    /**
     * 요청 ID로 발신자 정보 삭제
     */
    Mono<Void> deleteByRequestId(String requestId);
}
