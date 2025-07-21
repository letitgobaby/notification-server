package notification.application.service.processing.handler;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.port.outbound.persistence.NotificationRequestRepositoryPort;
import notification.application.service.infrastructure.saver.NotificationMessageWithOutboxSaver;
import notification.application.service.processing.parser.NotificationRequestParser;
import notification.definition.annotations.UnitOfWork;
import notification.domain.NotificationRequest;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestProcessingHandler {

    private final NotificationRequestRepositoryPort notificationRequestRepository;
    private final NotificationMessageWithOutboxSaver notificationMessageWithOutboxSaver;
    private final NotificationRequestParser notificationMessageParser;

    /**
     * NotificationRequest를 처리하고, 해당 요청에 대한 NotificationMessage를 생성하여 저장합니다.
     *
     * @param domain NotificationRequest
     * @return Mono<Void>
     */
    @UnitOfWork
    public Mono<NotificationRequest> handle(NotificationRequest domain) {
        log.info("Handling NotificationRequest: {}", domain.getRequestId().value());

        return Mono.just(domain)
                .doOnNext(NotificationRequest::markAsProcessing) // PROCESSING 상태로 변경
                .flatMap(notificationRequestRepository::save) // 변경사항 저장
                .flatMap(savedNotificationRequest -> {
                    return notificationMessageParser.parse(savedNotificationRequest) // 메시지 파싱
                            .flatMap(notificationMessageWithOutboxSaver::save) // 파싱된 메시지 Outbox 저장
                            .collectList() // 모든 Outbox 저장이 완료되기를 기다림
                            .thenReturn(savedNotificationRequest); // 원래 NotificationRequest 객체를 다음 flatMap으로 전달
                })
                .doOnNext(NotificationRequest::markAsDispatched) // DISPATCHED 상태로 변경
                .flatMap(notificationRequestRepository::save); // DISPATCHED 상태 저장
    }

}