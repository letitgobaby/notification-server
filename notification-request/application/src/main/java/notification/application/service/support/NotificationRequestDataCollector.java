package notification.application.service.support;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.service.TemplateRenderingService;
import notification.application.user.port.outbound.UserConfigProviderPort;
import notification.definition.vo.CollectedData;
import notification.definition.vo.RenderedContent;
import notification.definition.vo.TemplateRenderKey;
import notification.definition.vo.UserConfig;
import notification.domain.NotificationRequest;
import notification.domain.enums.NotificationType;
import notification.domain.vo.recipient.RecipientReference;
import notification.domain.vo.recipient.UserRecipient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 알림 처리에 필요한 데이터를 배치로 수집하는 서비스
 * 
 * UserConfig와 템플릿 렌더링 결과를 미리 수집하여
 * 중복 요청을 방지하고 성능을 최적화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestDataCollector {

    private final UserConfigProviderPort userConfigProviderPort;
    private final TemplateRenderingService templateRenderingService;

    private static final String DEFAULT_LANGUAGE = "ko";

    /**
     * 알림 요청에 필요한 모든 데이터를 수집합니다.
     * 
     * @param request 알림 요청
     * @return CollectedData 객체
     */
    public Mono<CollectedData<UserConfig>> collectData(NotificationRequest request) {
        return collectUserConfigs(request)
                .flatMap(userConfigs -> collectTemplates(request, userConfigs)
                        .map(templates -> new CollectedData<>(userConfigs, templates)));
    }

    /**
     * 필요한 모든 UserConfig를 배치로 수집합니다.
     */
    private Mono<Map<String, UserConfig>> collectUserConfigs(NotificationRequest request) {
        if (request.getRecipients() == null || request.getRecipients().isEmpty()) {
            return Mono.just(Map.of());
        }

        Set<String> userIds = request.getRecipients().stream()
                .filter(UserRecipient.class::isInstance)
                .map(UserRecipient.class::cast)
                .map(user -> user.userId().value())
                .collect(Collectors.toSet());

        return Flux.fromIterable(userIds)
                .flatMap(userId -> userConfigProviderPort.getUserConfigById(userId)
                        .map(config -> Map.entry(userId, config))
                        .onErrorResume(error -> {
                            log.warn("Failed to get user config for userId: {}, error: {}", userId, error.getMessage());
                            return Mono.empty();
                        }))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    /**
     * 필요한 모든 템플릿을 미리 렌더링합니다.
     */
    private Mono<Map<String, RenderedContent>> collectTemplates(
            NotificationRequest request, Map<String, UserConfig> userConfigs) {
        // 템플릿이 없는 경우 빈 맵 반환
        if (request.getTemplate() == null) {
            return Mono.just(Map.of());
        }

        // 템플릿 키 생성 및 렌더링
        Set<TemplateRenderKey> templateKeys = new HashSet<>();
        for (RecipientReference recipientRef : request.getRecipients()) {

            String language = DEFAULT_LANGUAGE; // 기본 언어 설정
            if (recipientRef instanceof UserRecipient userRecipient) {
                UserConfig config = userConfigs.get(userRecipient.userId().value());
                language = config != null && config.language() != null ? config.language() : DEFAULT_LANGUAGE;
            }

            // 모든 NotificationType에 대해 템플릿 키 생성
            for (NotificationType type : request.getNotificationTypes()) {
                templateKeys.add(new TemplateRenderKey(request.getTemplate().templateId(), type.name(), language));
            }
        }

        return Flux.fromIterable(templateKeys)
                .flatMap(key -> templateRenderingService
                        .renderTemplate(request.getTemplate(), NotificationType.valueOf(key.type()), key.language())
                        .map(rendered -> Map.entry(key.toString(), rendered))
                        .onErrorResume(error -> {
                            log.warn("Failed to render template for key: {}, error: {}", key, error.getMessage());
                            return Mono.empty();
                        }))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

}
