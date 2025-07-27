package notification.adapter.db.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import notification.adapter.db.NotificationRequestEntity;
import reactor.core.publisher.Mono;

/**
 * NotificationRequest Entity Repository
 */
public interface R2dbcNotificationRequestRepository extends ReactiveCrudRepository<NotificationRequestEntity, String> {

    /**
     * 상태별 알림 요청 조회
     */
    Mono<NotificationRequestEntity> findByRequestIdAndStatus(String requestId, String status);
}
