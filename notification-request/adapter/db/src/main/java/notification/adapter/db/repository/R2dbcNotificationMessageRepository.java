package notification.adapter.db.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import notification.adapter.db.NotificationMessageEntity;

public interface R2dbcNotificationMessageRepository extends R2dbcRepository<NotificationMessageEntity, String> {

}
