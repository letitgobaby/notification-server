package notification.adapter.db.adapter;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import notification.adapter.db.NotificationRequestEntity;
import notification.adapter.db.mapper.NotificationRequestEntityMapper;
import notification.adapter.db.repository.R2dbcNotificationRequestContentRepository;
import notification.adapter.db.repository.R2dbcNotificationRequestRecipientRepository;
import notification.adapter.db.repository.R2dbcNotificationRequestRepository;
import notification.adapter.db.repository.R2dbcNotificationRequestSenderRepository;
import notification.adapter.db.repository.R2dbcNotificationRequestTemplateInfoRepository;
import notification.application.notifiation.port.outbound.persistence.NotificationRequestRepositoryPort;
import notification.domain.NotificationRequest;
import notification.domain.vo.NotificationRequestId;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class NotificationRequestRepositoryAdapter implements NotificationRequestRepositoryPort {

    private final NotificationRequestEntityMapper mapper;

    private final R2dbcNotificationRequestRepository requestRepository;
    private final R2dbcNotificationRequestRecipientRepository recipientRepository;
    private final R2dbcNotificationRequestSenderRepository senderRepository;
    private final R2dbcNotificationRequestContentRepository contentRepository;
    private final R2dbcNotificationRequestTemplateInfoRepository templateInfoRepository;

    /**
     * NotificationRequest를 저장합니다.
     * 
     * @param domain 저장할 NotificationRequest 도메인 객체
     * @return 저장된 NotificationRequest 도메인 객체
     */
    @Override
    public Mono<NotificationRequest> save(NotificationRequest domain) {
        return Mono.fromCallable(() -> mapper.toEntity(domain))
                .flatMap(requestRepository::save)
                .flatMap(savedEntity -> {

                    // 모든 Mono들을 flatMap으로 연결하여 savedEntity를 계속 전달
                    return saveContent(domain, savedEntity)
                            .flatMap(entity -> saveTemplateInfo(domain, entity))
                            .flatMap(entity -> saveRecipients(domain, entity))
                            .flatMap(entity -> saveSenders(domain, entity))
                            .map(mapper::toDomain);
                });
    }

    /**
     * 컨텐츠 저장
     * 
     * @param domain
     * @param savedEntity
     * @return
     */
    private Mono<NotificationRequestEntity> saveContent(
            NotificationRequest domain, NotificationRequestEntity savedEntity) {
        if (domain.getContent() == null)
            return Mono.just(savedEntity);

        return Mono.fromCallable(() -> mapper.toContentEntity(
                domain, savedEntity.getRequestId(), savedEntity.getContentId()))
                .flatMap(contentRepository::save)
                .map(contentEntity -> {
                    savedEntity.setContent(contentEntity);
                    return savedEntity;
                }).defaultIfEmpty(savedEntity);
    }

    /**
     * 템플릿 정보 저장
     * 
     * @param domain
     * @param savedEntity
     * @return
     */
    public Mono<NotificationRequestEntity> saveTemplateInfo(
            NotificationRequest domain, NotificationRequestEntity savedEntity) {
        if (domain.getTemplate() == null)
            return Mono.just(savedEntity);

        return Mono.fromCallable(() -> mapper.toTemplateInfoEntity(
                domain, savedEntity.getRequestId(), savedEntity.getTemplateInfoId()))
                .flatMap(templateInfoRepository::save)
                .map(templateInfoEntity -> {
                    savedEntity.setTemplateInfo(templateInfoEntity);
                    return savedEntity;
                }).defaultIfEmpty(savedEntity);
    }

    /**
     * 수신자 저장
     * 
     * @param domain
     * @param savedEntity
     * @return
     */
    public Mono<NotificationRequestEntity> saveRecipients(
            NotificationRequest domain, NotificationRequestEntity savedEntity) {

        return Mono.fromCallable(() -> mapper.toRecipientEntities(domain, savedEntity.getRequestId()))
                .flatMapMany(recipientRepository::saveAll)
                .collectList()
                .flatMap(recipients -> {
                    savedEntity.setRecipients(recipients);
                    return Mono.just(savedEntity);
                });
    }

    /**
     * 발신자 저장
     * 
     * @param domain
     * @param savedEntity
     * @return
     */
    public Mono<NotificationRequestEntity> saveSenders(
            NotificationRequest domain, NotificationRequestEntity savedEntity) {

        return Mono.fromCallable(() -> mapper.toSenderEntities(domain, savedEntity.getRequestId()))
                .flatMapMany(senderRepository::saveAll)
                .collectList()
                .flatMap(senders -> {
                    savedEntity.setSenders(senders);
                    return Mono.just(savedEntity);
                });
    }

    @Override
    public Mono<NotificationRequest> findById(NotificationRequestId id) {
        return requestRepository.findById(id.value())
                .switchIfEmpty(Mono.empty())
                .flatMap(entity -> {
                    return Mono.zip(
                            recipientRepository.findByRequestId(id.value()).collectList(),
                            senderRepository.findByRequestId(id.value()).collectList(),
                            contentRepository.findByRequestId(id.value()),
                            templateInfoRepository.findByRequestId(id.value()) //
                    ).flatMap(tuple -> {
                        var content = tuple.getT3();
                        var templateInfo = tuple.getT4();
                        if (content == null || templateInfo == null) {
                            return Mono.error(new RuntimeException(
                                    "NotificationRequest content or templateInfo not found for id: " + id.value()));
                        }

                        entity.setRecipients(tuple.getT1());
                        entity.setSenders(tuple.getT2());
                        entity.setContent(content);
                        entity.setTemplateInfo(templateInfo);
                        return Mono.just(mapper.toDomain(entity));
                    });
                });
    }

    @Override
    public Mono<Void> deleteById(NotificationRequestId id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
    }

}
