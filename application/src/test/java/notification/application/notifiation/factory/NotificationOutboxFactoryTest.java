package notification.application.notifiation.factory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import notification.application.common.factory.JsonPayloadFactory;
import notification.domain.common.vo.JsonPayload;
import notification.domain.notification.NotificationOutbox;
import notification.domain.notification.NotificationRequest;
import notification.domain.notification.enums.OutboxStatus;
import notification.domain.notification.vo.NotificationId;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class NotificationOutboxFactoryTest {

    private JsonPayloadFactory jsonPayloadFactory;
    private NotificationOutboxFactory notificationOutboxFactory;

    @BeforeEach
    void setUp() {
        jsonPayloadFactory = mock(JsonPayloadFactory.class);
        notificationOutboxFactory = new NotificationOutboxFactory(jsonPayloadFactory);
    }

    @DisplayName("fronRequest - 입력값, 초기값 검증")
    @Test
    void fromRequest_create() {
        NotificationRequest request = mock(NotificationRequest.class);
        NotificationId notificationId = NotificationId.generate();
        JsonPayload jsonPayload = new JsonPayload("{\"key\":\"value\"}");

        when(request.getNotificationId()).thenReturn(notificationId);
        when(jsonPayloadFactory.toJsonPayload(request)).thenReturn(jsonPayload);

        Mono<NotificationOutbox> resultMono = notificationOutboxFactory.fromRequest(request);

        StepVerifier.create(resultMono).assertNext(outbox -> {
            assertNull(outbox.getOutboxId());
            assertEquals("NotificationRequest", outbox.getAggregateType());
            assertEquals(notificationId, outbox.getNotificationId());
            assertEquals("RequestedEvent", outbox.getMessageType());
            assertEquals(jsonPayload, outbox.getPayload());
            assertEquals(OutboxStatus.PENDING, outbox.getStatus());
            assertEquals(0, outbox.getRetryAttempts());
            assertNotNull(outbox.getNextRetryAt());
            assertNotNull(outbox.getCreatedAt());

            Instant now = Instant.now();
            assertTrue(!outbox.getNextRetryAt().isAfter(now));
            assertTrue(!outbox.getCreatedAt().isAfter(now));
        }).verifyComplete();
    }

    @DisplayName("fromRequest - JsonPayloadFactory 예외 전파")
    @Test
    void fromRequest_exceptionCatch() {
        NotificationRequest request = mock(NotificationRequest.class);
        RuntimeException ex = new RuntimeException("JSON error");
        when(jsonPayloadFactory.toJsonPayload(request)).thenThrow(ex);

        Mono<NotificationOutbox> resultMono = notificationOutboxFactory.fromRequest(request);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable == ex)
                .verify();
    }
}