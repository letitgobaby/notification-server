package notification.application.service.support;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.service.TemplateRenderingService;
import notification.application.user.dto.UserConfig;
import notification.application.user.port.outbound.UserConfigProviderPort;
import notification.definition.enums.NotificationType;
import notification.definition.vo.UserId;
import notification.domain.NotificationMessage;
import notification.domain.NotificationRequest;
import notification.domain.vo.NotificationChannelConfig;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationRequestDetails;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.TargetAudience;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageComposer {

    private final UserConfigProviderPort userConfigProviderPort;
    private final TemplateRenderingService templateRenderingService;

    /**
     * NotificationRequest를 NotificationMessage 리스트로 변환합니다.
     * 
     * @param request 알림 요청 정보
     * @return NotificationMessage 리스트
     */
    public Flux<NotificationMessage> composeMessages(NotificationRequest request) {
        NotificationRequestId requestId = request.getRequestId();
        NotificationRequestDetails details = request.getRequestDetails();
        List<NotificationChannelConfig> channelConfigs = details.channelConfigs();
        TargetAudience audience = request.getTargetAudience();
        Instant scheduledAt = request.getScheduledAt();

        // 사용자 ID를 기반으로 메시지를 생성합니다.
        Flux<NotificationMessage> userMessages = Flux.fromIterable(audience.userIds())
                .flatMap(userId -> getUserConfig(userId))
                .flatMap(userConfig -> {
                    return Flux.fromIterable(channelConfigs)
                            .filter(cfg -> userConfig.supportType(cfg.notificationType()))
                            .flatMap(channelConfig -> {
                                NotificationType type = channelConfig.notificationType();

                                return getRenderTemplate(details, type, userConfig.language())
                                        .map(content -> NotificationMessage.create(
                                                requestId,
                                                type,
                                                userConfig.toRecipient(type),
                                                content,
                                                channelConfig.senderInfo(),
                                                scheduledAt));
                            });
                });

        // 직접 수신자를 기반으로 메시지를 생성합니다.
        Flux<NotificationMessage> directMessages = Flux.fromIterable(audience.directRecipients())
                .flatMap(direct -> Flux.fromIterable(channelConfigs)
                        .filter(cfg -> direct.supportType(cfg.notificationType()))
                        .flatMap(cfg -> {
                            NotificationType type = cfg.notificationType();

                            return getRenderTemplate(details, type, null)
                                    .map(content -> NotificationMessage.create(
                                            requestId,
                                            type,
                                            direct,
                                            content,
                                            cfg.senderInfo(),
                                            scheduledAt));
                        }));

        return Flux.concat(userMessages, directMessages);
    }

    /**
     * 템플릿 정보를 기반으로 렌더링된 콘텐츠를 가져옵니다.
     * 
     * @param templateInfo 템플릿 정보
     * @param language     언어 코드
     * @return 렌더링된 콘텐츠 Mono
     */
    private Mono<NotificationContent> getRenderTemplate(
            NotificationRequestDetails details, NotificationType type, String language) {
        if (details.useTemplate()) {
            return templateRenderingService.renderTemplate(details.templateInfo(), type, language)
                    .map(NotificationContent::fromTemplate);
        } else {
            return Mono.just(details.directContent());
        }
    }

    /**
     * 사용자 ID로부터 UserConfig를 가져옵니다.
     * 
     * @param userId 사용자 ID
     * @return UserConfig Mono
     */
    private Mono<UserConfig> getUserConfig(UserId userId) {
        return userConfigProviderPort.getUserConfigById(userId.value());
    }

}
