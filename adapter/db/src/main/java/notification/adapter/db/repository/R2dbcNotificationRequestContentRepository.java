package notification.adapter.db.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import notification.adapter.db.NotificationRequestContentEntity;
import reactor.core.publisher.Mono;

public interface R2dbcNotificationRequestContentRepository
        extends R2dbcRepository<NotificationRequestContentEntity, String> {

    /**
     * 요청 ID로 NotificationRequestContent 조회
     *
     * @param requestId 요청 ID
     * @return NotificationRequestContentEntity
     */
    Mono<NotificationRequestContentEntity> findByRequestId(String requestId);

}
