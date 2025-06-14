package notification.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import notification.domain.common.exceptions.DomainValidationException;
import notification.domain.common.vo.JsonPayload;
import notification.domain.notification.NotificationOutbox;
import notification.domain.notification.enums.OutboxStatus;
import notification.domain.notification.exceptions.InvalidOutboxStatusException;
import notification.domain.notification.vo.NotificationId;

class NotificationOutboxTest {

        private Long outboxId;
        private NotificationId notificationId;
        private JsonPayload payload;
        private Instant createdAt;

        @BeforeEach
        void setUp() {
                outboxId = 1L;
                notificationId = NotificationId.generate();
                payload = new JsonPayload("{\"message\":\"test\"}");
                createdAt = Instant.now();
        }

        @DisplayName("AggregateType와 MessageType이 고정된 NotificationOutbox 생성")
        @Test
        void constructor_setsFixedAggregateTypeAndMessageType() {
                NotificationOutbox outbox = new NotificationOutbox(
                                outboxId, "ignored", notificationId, "ignored", payload, null, 0, null, createdAt);

                assertEquals("NotificationRequest", outbox.getAggregateType());
                assertEquals("RequestedEvent", outbox.getMessageType());
        }

        @DisplayName("NotificationOutbox 생성 시 필수 필드가 null인 경우 초기값 설정")
        @Test
        void constructor_setsDefaultsWhenNullOrInvalid() {
                NotificationOutbox outbox = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, null, -5, null, createdAt);

                assertEquals(OutboxStatus.PENDING, outbox.getStatus());
                assertEquals(0, outbox.getRetryAttempts());
                assertNotNull(outbox.getNextRetryAt());
        }

        @DisplayName("NotificationOutbox 생성 시 필수 필드가 null인 경우 예외 발생")
        @Test
        void constructor_throwsOnNullArguments() {
                assertThrows(DomainValidationException.class,
                                () -> new NotificationOutbox(null, null, notificationId, null, payload, null, 0, null,
                                                createdAt));
                assertThrows(DomainValidationException.class,
                                () -> new NotificationOutbox(outboxId, null, null, null, payload, null, 0, null,
                                                createdAt));
                assertThrows(DomainValidationException.class,
                                () -> new NotificationOutbox(outboxId, null, notificationId, null, null, null, 0, null,
                                                createdAt));
        }

        @DisplayName("markAsSent 메서드가 PENDING 또는 FAILED 상태에서만 상태를 SENT로 변경")
        @Test
        void markAsSent_changesStatusWhenPendingOrFailed() {
                NotificationOutbox outboxPending = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.PENDING, 0, Instant.now(),
                                createdAt);
                outboxPending.markAsSent();
                assertEquals(OutboxStatus.SENT, outboxPending.getStatus());

                NotificationOutbox outboxFailed = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.FAILED, 0, Instant.now(),
                                createdAt);
                outboxFailed.markAsSent();
                assertEquals(OutboxStatus.SENT, outboxFailed.getStatus());
        }

        @DisplayName("markAsSent 메서드가 SENT 상태에서 호출되면 예외 발생")
        @Test
        void markAsSent_throwsIfStatusIsSent() {
                NotificationOutbox outbox = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.SENT, 0, Instant.now(),
                                createdAt);
                assertThrows(InvalidOutboxStatusException.class, outbox::markAsSent);
        }

        @DisplayName("markAsFailed 메서드가 처리될때 retry 횟수와 상태를 업데이트")
        @Test
        void markAsFailed_updatesStatusAndRetryInfo() {
                NotificationOutbox outbox = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.PENDING, 2, Instant.now(),
                                createdAt);
                Instant nextRetry = Instant.now().plusSeconds(60);
                outbox.markAsFailed(nextRetry);

                assertEquals(OutboxStatus.FAILED, outbox.getStatus());
                assertEquals(3, outbox.getRetryAttempts());
                assertEquals(nextRetry, outbox.getNextRetryAt());
        }

        @DisplayName("markAsFailed 메서드가 null인 nextRetryAt을 받으면 예외 발생")
        @Test
        void markAsFailed_throwsIfNextRetryNull() {
                NotificationOutbox outbox = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.PENDING, 0, Instant.now(),
                                createdAt);
                assertThrows(DomainValidationException.class, () -> outbox.markAsFailed(null));
        }

        @DisplayName("isPending 메서드가 PENDING 상태에서만 true를 반환")
        @Test
        void isPending_returnsTrueOnlyIfPending() {
                NotificationOutbox outbox = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.PENDING, 0, Instant.now(),
                                createdAt);
                assertTrue(outbox.isPending());

                NotificationOutbox outbox2 = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.FAILED, 0, Instant.now(),
                                createdAt);
                assertFalse(outbox2.isPending());
        }

        @DisplayName("isReadyForRetry 메서드가 FAILED 상태이고, nextRetryAt이 현재 시각보다 이전이면 true를 반환")
        @Test
        void isReadyForRetry_returnsTrueOnlyIfFailedAndTimePassed() {
                Instant past = Instant.now().minusSeconds(10);
                NotificationOutbox outbox = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.FAILED, 0, past, createdAt);
                assertTrue(outbox.isReadyForRetry());

                NotificationOutbox outbox2 = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.FAILED, 0,
                                Instant.now().plusSeconds(100),
                                createdAt);
                assertFalse(outbox2.isReadyForRetry());

                NotificationOutbox outbox3 = new NotificationOutbox(
                                outboxId, null, notificationId, null, payload, OutboxStatus.PENDING, 0, past,
                                createdAt);
                assertFalse(outbox3.isReadyForRetry());
        }
}