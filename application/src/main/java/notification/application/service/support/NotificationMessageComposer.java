package notification.application.service.support;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.service.TemplateRenderingService;
import notification.application.user.dto.UserConfig;
import notification.application.user.port.outbound.UserConfigProviderPort;
import notification.domain.NotificationMessage;
import notification.domain.NotificationRequest;
import notification.domain.enums.NotificationType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.UserId;
import notification.domain.vo.recipient.AllUserRecipient;
import notification.domain.vo.recipient.DirectRecipient;
import notification.domain.vo.recipient.Recipient;
import notification.domain.vo.recipient.RecipientReference;
import notification.domain.vo.recipient.SegmentRecipient;
import notification.domain.vo.recipient.UserRecipient;
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
        return Flux.fromIterable(request.getRecipients())
                .flatMap(recipientRef -> expandRecipient(recipientRef))
                .flatMap(recipient -> createMessagesForRecipient(request, recipient));
    }

    /**
     * RecipientReference를 실제 Recipient로 확장합니다.
     * 
     * @param recipientRef 수신자 참조
     * @return 확장된 Recipient Flux
     */
    private Flux<Recipient> expandRecipient(RecipientReference recipientRef) {
        if (recipientRef instanceof UserRecipient userRecipient) {
            return getUserConfig(userRecipient.userId())
                    .map(this::toRecipient)
                    .flux();
        } else if (recipientRef instanceof DirectRecipient directRecipient) {
            return Flux.just(new Recipient(
                    null,
                    directRecipient.emailAddress(),
                    directRecipient.phoneNumber(),
                    directRecipient.deviceToken(),
                    "ko" // 기본 언어
            ));
        } else if (recipientRef instanceof AllUserRecipient) {
            return getAllUserConfigs().map(this::toRecipient);
        } else if (recipientRef instanceof SegmentRecipient segmentRecipient) {
            return getSegmentUserConfigs(segmentRecipient.segmentName())
                    .map(this::toRecipient);
        } else {
            return Flux.error(new IllegalArgumentException("Unsupported recipient type: " + recipientRef.getClass()));
        }
    }

    /**
     * 특정 수신자에 대해 모든 알림 채널의 메시지를 생성합니다.
     * 
     * @param request   알림 요청
     * @param recipient 수신자
     * @return 생성된 NotificationMessage Flux
     */
    private Flux<NotificationMessage> createMessagesForRecipient(NotificationRequest request, Recipient recipient) {
        return Flux.fromIterable(request.getNotificationTypes())
                .filter(type -> canReceiveNotification(recipient, type))
                .flatMap(type -> createMessage(request, recipient, type));
    }

    /**
     * 단일 NotificationMessage를 생성합니다.
     * 
     * @param request   알림 요청
     * @param recipient 수신자
     * @param type      알림 채널
     * @return 생성된 NotificationMessage Mono
     */
    private Mono<NotificationMessage> createMessage(NotificationRequest request, Recipient recipient,
            NotificationType type) {
        return getNotificationContent(request, type, recipient.language())
                .map(content -> NotificationMessage.create(
                        request.getRequestId(),
                        type,
                        recipient,
                        content,
                        request.getSenderInfos().get(type),
                        request.getScheduledAt()));
    }

    /**
     * 알림 내용을 가져옵니다 (템플릿 렌더링 또는 직접 콘텐츠).
     * 
     * @param request  알림 요청
     * @param type     알림 채널
     * @param language 언어 코드
     * @return 렌더링된 콘텐츠 Mono
     */
    private Mono<NotificationContent> getNotificationContent(NotificationRequest request, NotificationType type,
            String language) {
        if (request.getTemplate() != null) {
            // 템플릿 기반 콘텐츠
            return templateRenderingService.renderTemplate(request.getTemplate(), type, language)
                    .map(renderedContent -> new NotificationContent(
                            renderedContent.title(),
                            renderedContent.body(),
                            request.getContent().redirectUrl(),
                            request.getContent().imageUrl() //
                    ));
        } else {
            // 직접 작성된 콘텐츠
            return Mono.just(request.getContent());
        }
    }

    /**
     * 수신자가 특정 알림 채널을 수신할 수 있는지 확인합니다.
     * 
     * @param recipient 수신자
     * @param type      알림 채널
     * @return 수신 가능 여부
     */
    private boolean canReceiveNotification(Recipient recipient, NotificationType type) {
        if (type == NotificationType.EMAIL) {
            return recipient.email() != null && !recipient.email().isBlank();
        } else if (type == NotificationType.SMS) {
            return recipient.phoneNumber() != null && !recipient.phoneNumber().isBlank();
        } else if (type == NotificationType.PUSH) {
            return recipient.deviceToken() != null && !recipient.deviceToken().isBlank();
        }
        return false;
    }

    /**
     * UserConfig를 Recipient로 변환합니다.
     * 
     * @param userConfig 사용자 설정
     * @return Recipient 객체
     */
    private Recipient toRecipient(UserConfig userConfig) {
        return new Recipient(
                userConfig.userId(),
                userConfig.email(),
                userConfig.phoneNumber(),
                userConfig.pushToken(), // pushToken 필드 사용
                userConfig.language());
    }

    /**
     * 특정 사용자의 설정 정보를 가져옵니다.
     * 
     * @param userId 사용자 ID
     * @return UserConfig Mono
     */
    private Mono<UserConfig> getUserConfig(UserId userId) {
        return userConfigProviderPort.getUserConfigById(userId.value());
    }

    /**
     * 전체 사용자의 설정 정보를 가져옵니다.
     * 
     * @return UserConfig Flux
     */
    private Flux<UserConfig> getAllUserConfigs() {
        // TODO: 실제 구현에서는 UserConfigProviderPort에 getAllUserConfigs() 메서드 추가 필요
        // 임시로 빈 Flux 반환
        return Flux.empty();
    }

    /**
     * 특정 세그먼트에 속한 사용자들의 설정 정보를 가져옵니다.
     * 
     * @param segmentName 세그먼트 이름
     * @return UserConfig Flux
     */
    private Flux<UserConfig> getSegmentUserConfigs(String segmentName) {
        // TODO: 실제 구현에서는 UserConfigProviderPort에 getUserConfigsBySegment() 메서드 추가 필요
        // 임시로 빈 Flux 반환
        return Flux.empty();
    }

}
