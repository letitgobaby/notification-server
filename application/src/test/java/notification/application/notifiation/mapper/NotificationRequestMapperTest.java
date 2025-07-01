package notification.application.notifiation.mapper;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.ContentCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.DirectRecipientCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.RecipientsCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.RequesterCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.SenderInfoCommand;
import notification.application.notifiation.dto.NotificationRequestCommand.TemplateCommand;
import notification.domain.enums.NotificationType;
import notification.domain.enums.RequesterType;
import notification.domain.vo.recipient.AllUserRecipient;
import notification.domain.vo.recipient.DirectRecipient;
import notification.domain.vo.recipient.SegmentRecipient;
import notification.domain.vo.recipient.UserRecipient;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SmsSender;
import reactor.test.StepVerifier;

class NotificationRequestMapperTest {

    private NotificationRequestMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NotificationRequestMapper();
    }

    @Nested
    @DisplayName("fromCommand 메서드 테스트")
    class FromCommandTest {

        @Test
        @DisplayName("null command가 입력되면 IllegalArgumentException이 발생한다")
        void shouldThrowExceptionWhenCommandIsNull() {
            // when & then
            StepVerifier.create(mapper.fromCommand(null))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }

        @Test
        @DisplayName("회원 사용자 대상 템플릿 기반 이메일+푸시 알림을 정상적으로 매핑한다")
        void shouldMapUserRecipientsWithTemplate() {
            // given
            var command = createTemplateBasedCommand();

            // when & then
            StepVerifier.create(mapper.fromCommand(command))
                    .assertNext(request -> {
                        assertThat(request).isNotNull();
                        assertThat(request.getRequester().type()).isEqualTo(RequesterType.SERVICE);
                        assertThat(request.getRequester().id()).isEqualTo("marketing-service");

                        assertThat(request.getRecipients()).hasSize(2);
                        assertThat(request.getRecipients().get(0)).isInstanceOf(UserRecipient.class);
                        assertThat(request.getRecipients().get(1)).isInstanceOf(UserRecipient.class);

                        assertThat(request.getNotificationTypes()).containsExactly(
                                NotificationType.EMAIL, NotificationType.PUSH);

                        assertThat(request.getSenderInfos()).hasSize(2);
                        assertThat(request.getSenderInfos().get(NotificationType.EMAIL))
                                .isInstanceOf(EmailSender.class);
                        assertThat(request.getSenderInfos().get(NotificationType.PUSH))
                                .isInstanceOf(PushSender.class);

                        assertThat(request.getTemplate()).isNotNull();
                        assertThat(request.getTemplate().templateId()).isEqualTo("WELCOME_EMAIL");

                        assertThat(request.getMemo()).isEqualTo("신규 사용자 환영 이메일");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("직접 수신자 대상 콘텐츠 기반 SMS 알림을 정상적으로 매핑한다")
        void shouldMapDirectRecipientsWithContent() {
            // given
            var command = createContentBasedCommand();

            // when & then
            StepVerifier.create(mapper.fromCommand(command))
                    .assertNext(request -> {
                        assertThat(request).isNotNull();
                        assertThat(request.getRequester().type()).isEqualTo(RequesterType.ADMIN);

                        assertThat(request.getRecipients()).hasSize(2);
                        assertThat(request.getRecipients().get(0)).isInstanceOf(DirectRecipient.class);
                        assertThat(request.getRecipients().get(1)).isInstanceOf(DirectRecipient.class);

                        var directRecipient1 = (DirectRecipient) request.getRecipients().get(0);
                        assertThat(directRecipient1.emailAddress()).isEqualTo("user@example.com");
                        assertThat(directRecipient1.phoneNumber()).isEqualTo("010-1234-5678");

                        assertThat(request.getNotificationTypes()).containsExactly(NotificationType.SMS);

                        assertThat(request.getSenderInfos().get(NotificationType.SMS))
                                .isInstanceOf(SmsSender.class);

                        assertThat(request.getContent()).isNotNull();
                        assertThat(request.getContent().title()).isEqualTo("긴급 공지");
                        assertThat(request.getContent().body()).isEqualTo("시스템 점검이 예정되어 있습니다.");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("전체 사용자 대상 알림을 정상적으로 매핑한다")
        void shouldMapAllUsersRecipient() {
            // given
            var command = createAllUsersCommand();

            // when & then
            StepVerifier.create(mapper.fromCommand(command))
                    .assertNext(request -> {
                        assertThat(request.getRecipients()).hasSize(1);
                        assertThat(request.getRecipients().get(0)).isInstanceOf(AllUserRecipient.class);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("세그먼트 대상 알림을 정상적으로 매핑한다")
        void shouldMapSegmentRecipient() {
            // given
            var command = createSegmentCommand();

            // when & then
            StepVerifier.create(mapper.fromCommand(command))
                    .assertNext(request -> {
                        assertThat(request.getRecipients()).hasSize(1);
                        assertThat(request.getRecipients().get(0)).isInstanceOf(SegmentRecipient.class);

                        var segmentRecipient = (SegmentRecipient) request.getRecipients().get(0);
                        assertThat(segmentRecipient.segmentName()).isEqualTo("VIP_CUSTOMERS");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("예약 알림을 정상적으로 매핑한다")
        void shouldMapScheduledNotification() {
            // given
            var scheduledTime = Instant.parse("2025-07-01T09:00:00Z");
            var command = createScheduledCommand(scheduledTime);

            // when & then
            StepVerifier.create(mapper.fromCommand(command))
                    .assertNext(request -> {
                        assertThat(request.getScheduledAt()).isEqualTo(scheduledTime);
                    })
                    .verifyComplete();
        }
    }

    // Test Data Builders
    private NotificationRequestCommand createTemplateBasedCommand() {
        return new NotificationRequestCommand(
                new RequesterCommand(RequesterType.SERVICE, "marketing-service"),
                new RecipientsCommand(
                        List.of("user-001", "user-002"),
                        null,
                        null,
                        null),
                List.of(NotificationType.EMAIL, NotificationType.PUSH),
                Map.of(
                        NotificationType.EMAIL, new SenderInfoCommand(
                                null, "no-reply@example.com", "Marketing Team"),
                        NotificationType.PUSH, new SenderInfoCommand(
                                null, null, "MyApp")),
                null, // content는 null (템플릿 사용)
                new TemplateCommand("WELCOME_EMAIL", Map.of("userName", "홍길동")),
                null,
                "신규 사용자 환영 이메일");
    }

    private NotificationRequestCommand createContentBasedCommand() {
        return new NotificationRequestCommand(
                new RequesterCommand(RequesterType.ADMIN, "admin-001"),
                new RecipientsCommand(
                        null,
                        List.of(
                                new DirectRecipientCommand("010-1234-5678", "user@example.com", null),
                                new DirectRecipientCommand("010-9876-5432", "admin@example.com", null)),
                        null,
                        null),
                List.of(NotificationType.SMS),
                Map.of(NotificationType.SMS, new SenderInfoCommand(
                        "02-1234-5678", null, "시스템관리팀")),
                new ContentCommand("긴급 공지", "시스템 점검이 예정되어 있습니다.",
                        "https://example.com/notice", null),
                null, // template는 null (콘텐츠 사용)
                null,
                "시스템 점검 안내");
    }

    private NotificationRequestCommand createAllUsersCommand() {
        return new NotificationRequestCommand(
                new RequesterCommand(RequesterType.ADMIN, "system"),
                new RecipientsCommand(null, null, null, true),
                List.of(NotificationType.PUSH),
                Map.of(NotificationType.PUSH, new SenderInfoCommand(
                        null, null, "시스템")),
                new ContentCommand("공지사항", "새로운 기능이 추가되었습니다.", null, null),
                null,
                null,
                "전체 공지");
    }

    private NotificationRequestCommand createSegmentCommand() {
        return new NotificationRequestCommand(
                new RequesterCommand(RequesterType.SERVICE, "crm-service"),
                new RecipientsCommand(null, null, "VIP_CUSTOMERS", null),
                List.of(NotificationType.EMAIL),
                Map.of(NotificationType.EMAIL, new SenderInfoCommand(
                        null, "vip@example.com", "VIP팀")),
                null,
                new TemplateCommand("VIP_PROMOTION", Map.of("discountRate", "20%")),
                null,
                "VIP 고객 프로모션");
    }

    private NotificationRequestCommand createScheduledCommand(Instant scheduledAt) {
        return new NotificationRequestCommand(
                new RequesterCommand(RequesterType.USER, "user-123"),
                new RecipientsCommand(List.of("user-456"), null, null, null),
                List.of(NotificationType.PUSH),
                Map.of(NotificationType.PUSH, new SenderInfoCommand(
                        null, null, "개인알림")),
                new ContentCommand("리마인더", "약속 시간입니다.", null, null),
                null,
                scheduledAt,
                "개인 리마인더");
    }
}