package notification.application.service.support;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.common.JsonPayloadFactory;
import notification.application.notifiation.port.outbound.persistence.NotificationRequestRepositoryPort;
import notification.application.notifiation.port.outbound.persistence.RequestOutboxMessageRepositoryPort;
import notification.domain.NotificationRequest;
import notification.domain.RequestOutboxMessage;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestWithOutboxSaver {

    private final NotificationRequestRepositoryPort requestRepository;
    private final RequestOutboxMessageRepositoryPort outboxRepository;
    private final JsonPayloadFactory jsonPayloadFactory;

    /**
     * NotificationRequest를 저장하고, 해당 요청에 대한 Outbox 메시지를 생성하여 저장합니다.
     * 
     * @param request NotificationRequest 객체
     * @return 저장된 RequestOutboxMessage 객체를 포함하는 Mono
     */
    public Mono<RequestOutboxMessage> save(NotificationRequest request) {
        return requestRepository.save(request)
                .flatMap(saved -> {
                    RequestOutboxMessage outbox = RequestOutboxMessage.create(
                            saved.getClass().getName(),
                            saved.getRequestId().value(),
                            "NotificationRequestReceivedEvent",
                            jsonPayloadFactory.toJsonPayload(saved),
                            saved.getScheduledAt());
                    return outboxRepository.save(outbox);
                });
    }

}
