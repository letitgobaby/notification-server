package notification.application.service.support;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.definition.vo.CollectedData;
import notification.definition.vo.UserConfig;
import notification.domain.NotificationMessage;
import notification.domain.NotificationRequest;
import notification.domain.enums.NotificationType;
import notification.domain.vo.recipient.AllUserRecipient;
import notification.domain.vo.recipient.DirectRecipient;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.recipient.RecipientReference;
import notification.domain.vo.recipient.SegmentRecipient;
import notification.domain.vo.recipient.UserRecipient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 알림 요청을 파싱하여 NotificationMessage 스트림으로 변환하는 서비스
 * 
 * NotificationDataCollector를 사용하여 필요한 데이터를 수집하고,
 * 수집된 데이터로 메시지를 빌드합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRequestParser {

    private final NotificationRequestDataCollector dataCollector;
    private final NotificationContentBuilder contentBuilder;

    /**
     * 알림 요청을 파싱하여 NotificationMessage 스트림으로 변환합니다.
     * 
     * @param request 알림 요청
     * @return NotificationMessage 스트림
     */
    public Flux<NotificationMessage> parse(NotificationRequest request) {
        log.info("Starting notification parsing for request: {}, recipients: {}",
                request.getRequestId(), request.getRecipients().size());

        return dataCollector.collectData(request)
                .flatMapMany(data -> Flux.fromIterable(request.getRecipients())
                        .flatMap(recipientRef -> resolveRecipient(recipientRef, data.userConfigs()))
                        .flatMap(recipient -> Flux.fromIterable(request.getNotificationTypes())
                                .flatMap(type -> buildMessage(request, recipient, type, data))));
    }

    /**
     * RecipientReference를 Recipient로 변환합니다.
     */
    private Mono<Recipient> resolveRecipient(RecipientReference ref, Map<String, UserConfig> userConfigs) {
        if (ref instanceof UserRecipient user) {
            UserConfig config = userConfigs.get(user.userId().value());
            if (config == null) {
                log.warn("UserConfig not found for userId: {}, skipping", user.userId().value());
                return Mono.empty();
            }

            return Mono.just(new Recipient(
                    user.userId().value(),
                    config.email(), config.phoneNumber(),
                    config.pushToken(), config.language()));

        } else if (ref instanceof DirectRecipient direct) {
            return Mono.just(new Recipient(
                    null,
                    direct.emailAddress(), direct.phoneNumber(),
                    direct.deviceToken(), "ko"));

        } else if (ref instanceof AllUserRecipient) {
            log.warn("AllUserRecipient not yet supported");
            return Mono.empty();

        } else if (ref instanceof SegmentRecipient segment) {
            log.warn("SegmentRecipient not yet supported for segment: {}", segment.segmentName());
            return Mono.empty();
        }

        return Mono.error(new IllegalArgumentException("Unsupported recipient type: " + ref.getClass()));
    }

    /**
     * 단일 메시지를 빌드합니다.
     */
    private Mono<NotificationMessage> buildMessage(NotificationRequest request, Recipient recipient,
            NotificationType type, CollectedData<UserConfig> data) {

        return Mono.fromCallable(() -> contentBuilder.createContent(request, recipient, type, data))
                .map(content -> NotificationMessage.create(
                        request.getRequestId(),
                        type,
                        recipient,
                        content,
                        request.getSenderInfos().get(type),
                        request.getScheduledAt()));
    }

}