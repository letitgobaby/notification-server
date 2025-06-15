package notification.application.notifiation.factory;

import java.time.Instant;

import org.springframework.stereotype.Component;

import notification.application.common.factory.JsonPayloadFactory;
import notification.domain.notification.NotificationOutbox;
import notification.domain.notification.NotificationRequest;
import notification.domain.notification.enums.OutboxStatus;
import reactor.core.publisher.Mono;

@Component
public final class NotificationOutboxFactory {

    private final JsonPayloadFactory jsonPayloadFactory;

    public NotificationOutboxFactory(JsonPayloadFactory jsonPayloadFactory) {
        this.jsonPayloadFactory = jsonPayloadFactory;
    }

    //
    public Mono<NotificationOutbox> fromRequest(NotificationRequest request) {
        return Mono.fromCallable(() -> jsonPayloadFactory.toJsonPayload(request))
                .map(jsonPayload -> new NotificationOutbox(
                        null, // outboxId는 DB에서 자동 생성되므로 null로 설정
                        "NotificationRequest", // 애그리거트 타입은 NotificationRequest로 고정
                        request.getNotificationId(), // 요청의 NotificationId 사용
                        "RequestedEvent", // 발행될 메시지 타입은 RequestedEvent로 고정
                        jsonPayload, // 요청을 JSON 형태로 변환하여 payload로 사용
                        OutboxStatus.PENDING, // 상태는 기본값인 PENDING으로 설정
                        0, // 초기 재시도 횟수는 0
                        Instant.now(), // 초기에는 즉시 재시도 예정 시각을 현재 시각으로 설정
                        Instant.now() // 요청의 생성 시각 사용
                ));
    }

}
