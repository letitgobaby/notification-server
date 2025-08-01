package notification.adapter.db.adapter;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import notification.adapter.db.mapper.NotificationMessageEntityMapper;
import notification.adapter.db.repository.R2dbcNotificationMessageRepository;
import notification.application.notifiation.port.outbound.persistence.NotificationMessageRepositoryPort;
import notification.domain.NotificationMessage;
import notification.domain.vo.NotificationMessageId;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class NotificationMessageRepositoryAdapter implements NotificationMessageRepositoryPort {

    private final NotificationMessageEntityMapper mapper;
    private final R2dbcNotificationMessageRepository messageRepository;

    @Override
    public Mono<NotificationMessage> save(NotificationMessage domain) {
        return Mono.fromCallable(() -> mapper.toEntity(domain))
                .flatMap(messageRepository::save)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<NotificationMessage> findById(NotificationMessageId id) {
        return messageRepository.findById(id.value())
                .switchIfEmpty(Mono.empty())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(NotificationMessageId id) {
        return messageRepository.deleteById(id.value())
                .then();
    }

}
