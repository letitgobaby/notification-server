package notification.application.service.support;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import notification.application.service.TemplateRenderingService;
import notification.application.user.dto.UserConfig;
import notification.application.user.port.outbound.UserConfigProviderPort;
import notification.definition.enums.AudienceType;
import notification.definition.enums.NotificationType;
import notification.definition.enums.RequesterType;
import notification.definition.vo.UserId;
import notification.domain.NotificationRequest;
import notification.domain.vo.NotificationChannelConfig;
import notification.domain.vo.NotificationRequestDetails;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Recipient;
import notification.domain.vo.RenderedContent;
import notification.domain.vo.Requester;
import notification.domain.vo.SenderInfo;
import notification.domain.vo.TargetAudience;
import notification.domain.vo.TemplateId;
import notification.domain.vo.TemplateInfo;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class NotificationMessageComposerTest {

    private UserConfigProviderPort userConfigProviderPort;
    private TemplateRenderingService templateRenderingService;
    private NotificationMessageComposer composer;

    NotificationRequest mockRequest;

    @BeforeEach
    void setUp() {
        userConfigProviderPort = Mockito.mock(UserConfigProviderPort.class);
        templateRenderingService = Mockito.mock(TemplateRenderingService.class);
        composer = new NotificationMessageComposer(userConfigProviderPort, templateRenderingService);

        mockRequest = null; // 초기화
    }

    private NotificationRequest createMockRequest_with_6_messages() {
        return NotificationRequest.create(
                // RequestId
                new NotificationRequestId("req-1"),
                // Requester
                new Requester(RequesterType.SERVICE, "event-marketing-service"),
                // TargetAudience (MIXED)
                new TargetAudience(
                        AudienceType.MIXED,
                        Set.of(
                                new UserId("user-registered-a"),
                                new UserId("user-registered-b")),
                        null,
                        List.of(
                                Recipient.ofEmail("guest1@example.com"),
                                Recipient.ofPush("non-member-device-token-xyz-1"))),
                // RequestDetails
                new NotificationRequestDetails(
                        true, // useTemplate
                        new TemplateInfo(
                                new TemplateId("TEST_TEMPLATE"),
                                Map.of(
                                        "productName", "알림 테스트 제품",
                                        "launchDate", "변수 테스트")),
                        null, // directContent
                        List.of(
                                new NotificationChannelConfig(
                                        NotificationType.EMAIL,
                                        SenderInfo.ofEmailSender(
                                                "no-reply@myproduct.com",
                                                "마케팅팀")),
                                new NotificationChannelConfig(
                                        NotificationType.PUSH,
                                        SenderInfo.ofPushSender("마케팅팀")))),
                null // scheduledAt
        );
    }

    @DisplayName("toMessages 메세지 생성 테스트 - MIXED type")
    @Test
    void toMessages_shouldReturnMessagesForUserIds() {
        mockRequest = createMockRequest_with_6_messages();

        UserConfig userA = new UserConfig("user-registered-a", "user-A",
                "push-token-a", "a@mail.com", "123-123-123",
                "en", null);
        UserConfig userB = new UserConfig("user-registered-b", "user-B",
                "push-token-b", "b@mail.com", "456-456-456",
                "ko", null);
        when(userConfigProviderPort.getUserConfigById(eq("user-registered-a"))).thenReturn(Mono.just(userA));
        when(userConfigProviderPort.getUserConfigById(eq("user-registered-b"))).thenReturn(Mono.just(userB));

        RenderedContent en = new RenderedContent("title", "Rendered Body", null, null);
        RenderedContent ko = new RenderedContent("제목", "렌더링된 본문", null, null);
        when(templateRenderingService.renderTemplate(any(), any(), eq("en"))).thenReturn(Mono.just(en));
        when(templateRenderingService.renderTemplate(any(), any(), eq("ko"))).thenReturn(Mono.just(ko));
        when(templateRenderingService.renderTemplate(any(), any(), isNull())).thenReturn(Mono.just(en));

        //
        StepVerifier.create(composer.composeMessages(mockRequest))
                .expectNextCount(6) // (2 회원 * (PUSH, EMAIL)) + 2 비회원
                .assertNext(msg -> {
                    assertNotNull(msg);
                    assertThat(msg.getNotificationRequestId())
                            .isEqualTo(mockRequest.getRequestId());
                    assertThat(msg.getNotificationType()).isIn(NotificationType.EMAIL,
                            NotificationType.PUSH);
                    assertThat(msg.getRecipient()).isNotNull();
                    assertThat(msg.getNotificationContent()).isNotNull();
                });
    }

    @Test
    void toMessages_shouldReturnEmptyWhenNoAudience() {
        //
        NotificationRequestDetails details = Mockito.mock(NotificationRequestDetails.class);
        given(details.channelConfigs()).willReturn(List.of());
        given(details.useTemplate()).willReturn(false);

        TargetAudience audience = Mockito.mock(TargetAudience.class);
        given(audience.userIds()).willReturn(Set.of());
        given(audience.directRecipients()).willReturn(List.of());

        NotificationRequest request = Mockito.mock(NotificationRequest.class);
        given(request.getRequestDetails()).willReturn(details);
        given(request.getTargetAudience()).willReturn(audience);

        //
        StepVerifier.create(composer.composeMessages(request))
                .verifyComplete();
    }

}
