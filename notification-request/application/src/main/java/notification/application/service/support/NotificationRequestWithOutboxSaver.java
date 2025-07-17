package notification.application.service.support;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.JsonPayloadFactory;
import notification.application.notifiation.port.outbound.persistence.NotificationRequestRepositoryPort;
import notification.application.outbox.port.outbound.RequestOutboxRepositoryPort;
import notification.definition.vo.outbox.RequestOutbox;
import notification.domain.NotificationRequest;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestWithOutboxSaver {

    private final NotificationRequestRepositoryPort requestRepository;
    private final RequestOutboxRepositoryPort outboxRepository;
    private final JsonPayloadFactory jsonPayloadFactory;

    /**
     * NotificationRequest를 저장하고, 해당 요청에 대한 Outbox 메시지를 생성하여 저장합니다.
     * 
     * @param request NotificationRequest 객체
     * @return 저장된 RequestMessageOutbox 객체를 포함하는 Mono
     */
    public Mono<RequestOutbox> save(NotificationRequest request) {
        return requestRepository.save(request).flatMap(saved -> {
            RequestOutbox outbox = RequestOutbox.create(
                    saved.getRequestId().value(),
                    jsonPayloadFactory.toJsonPayload(saved),
                    saved.getScheduledAt());
            return outboxRepository.save(outbox);
        });
    }

}
