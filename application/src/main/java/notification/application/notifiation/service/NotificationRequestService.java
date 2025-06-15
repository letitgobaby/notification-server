package notification.application.notifiation.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestResult;
import notification.application.notifiation.factory.NotificationOutboxFactory;
import notification.application.notifiation.mapper.NotificationCommandMapper;
import notification.application.notifiation.port.inbound.NotificationRequestUseCase;
import notification.application.notifiation.port.outbound.NotificationOutboxRepositoryPort;
import notification.application.notifiation.port.outbound.NotificationRequestRepositoryPort;
import notification.application.notifiation.port.outbound.TransactionExecutorPort;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class NotificationRequestService implements NotificationRequestUseCase {

    private final TransactionExecutorPort transactionExecutor;
    private final NotificationRequestRepositoryPort notificationRequestRepository;
    private final NotificationOutboxRepositoryPort notificationOutboxRepository;
    private final NotificationOutboxFactory notificationOutboxFactory;

    public NotificationRequestService(
            TransactionExecutorPort transactionExecutor,
            NotificationRequestRepositoryPort notificationRequestRepository,
            NotificationOutboxRepositoryPort notificationOutboxRepository,
            NotificationOutboxFactory notificationOutboxFactory) {
        this.transactionExecutor = transactionExecutor;
        this.notificationRequestRepository = notificationRequestRepository;
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.notificationOutboxFactory = notificationOutboxFactory;
    }

    @Override
    public Mono<NotificationRequestResult> handle(NotificationRequestCommand command) {
        log.info("Handling notification request: {}", command);

        return transactionExecutor.executeTransaction(v -> { // 트랜잭션 시작 (Adapter에서 트랜잭션을 구현)
            return Mono.fromCallable(() -> NotificationCommandMapper.toDomain(command))
                    .flatMap(notificationRequestRepository::save)
                    .flatMap(notificationOutboxFactory::fromRequest)
                    .flatMap(notificationOutboxRepository::save)
                    .map(outbox -> {
                        log.info("Notification request processed successfully: {}", outbox);

                        return new NotificationRequestResult(outbox.getNotificationId(),
                                "SUCCESS",
                                "Notification request has been successfully processed");
                    });
        }).onErrorResume(e -> {
            log.error("Error processing notification request: {}", e.getMessage(), e);

            return Mono.just(new NotificationRequestResult(null, // 실패 시 NotificationId는 null
                    "FAILURE",
                    "Failed to process notification request: " + e.getMessage()));
        });

    }

}
