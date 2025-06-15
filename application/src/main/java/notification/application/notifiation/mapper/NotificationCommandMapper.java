package notification.application.notifiation.mapper;

import java.time.Instant;

import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.domain.notification.NotificationOutbox;
import notification.domain.notification.NotificationRequest;
import notification.domain.notification.enums.Priority;
import notification.domain.notification.vo.NotificationId;

public final class NotificationCommandMapper {

    public static NotificationRequest toDomain(NotificationRequestCommand command) {
        return new NotificationRequest(
                NotificationId.generate(), // NotificationId는 생성 시 자동으로 생성
                command.requesterId(),
                command.requesterType(),
                command.type(),
                command.audienceType(),
                command.recipient(),
                command.content(),
                command.scheduledAt() == null ? Instant.now() : command.scheduledAt(), // 예약 발송 시간은 현재 시각으로 설정
                command.priority() == null ? Priority.LOW : command.priority(), // 기본값은 LOW
                Instant.now() // 요청 시각은 현재 시각으로 설정
        );
    }

    public static NotificationOutbox toDomain() {
        return null;
    }

}
