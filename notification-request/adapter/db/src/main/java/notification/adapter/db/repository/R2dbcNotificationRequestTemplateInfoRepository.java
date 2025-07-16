package notification.adapter.db.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import notification.adapter.db.NotificationRequestTemplateInfoEntity;
import reactor.core.publisher.Mono;

public interface R2dbcNotificationRequestTemplateInfoRepository
        extends R2dbcRepository<NotificationRequestTemplateInfoEntity, String> {

    /**
     * 요청 ID로 NotificationRequestTemplateInfo 조회
     */
    Mono<NotificationRequestTemplateInfoEntity> findByRequestId(String requestId);

}
