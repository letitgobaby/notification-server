package notification.adapter.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import notification.adapter.web.constants.MyHttpHeaders;
import notification.adapter.web.dto.request.NotificationCreateRequest;
import notification.adapter.web.mapper.NotificationCreateRequestMapper;
import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestResult;
import notification.application.notifiation.port.inbound.NotificationRequestReceviedUseCase;
import notification.domain.enums.NotificationType;
import notification.domain.enums.RequesterType;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class NotificationRequestControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private NotificationRequestReceviedUseCase notificationRequestReceviedService;
    @Mock
    private NotificationCreateRequestMapper notificationCreateRequestMapper;

    private String idempotencyKey = "test-key";

    @BeforeEach
    void setUp() {
        NotificationRequestController controller = new NotificationRequestController(
                notificationRequestReceviedService, notificationCreateRequestMapper);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void handleNotificationRequest_success() {
        // given
        NotificationRequestCommand command = createMockCommand();
        NotificationRequestResult result = NotificationRequestResult.success("nid-123");

        when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class))).thenReturn(command);
        when(notificationRequestReceviedService.handle(eq(command), eq(idempotencyKey)))
                .thenReturn(Mono.just(result));

        // when & then
        webTestClient.post()
                .uri("/api/v1/notifications")
                .header(MyHttpHeaders.IDEMPOTENCY_KEY, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createTestRequest())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.notificationId").isEqualTo("nid-123")
                .jsonPath("$.status").isEqualTo("SUCCESS");
    }

    @Test
    void handleNotificationRequest_failure() {
        // given
        NotificationRequestCommand command = createMockCommand();

        when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class))).thenReturn(command);
        when(notificationRequestReceviedService.handle(eq(command), eq(idempotencyKey)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // when & then
        webTestClient.post()
                .uri("/api/v1/notifications")
                .header(MyHttpHeaders.IDEMPOTENCY_KEY, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createTestRequest())
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void handleNotificationRequest_missingIdempotencyKey() {
        // when & then
        webTestClient.post()
                .uri("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createTestRequest())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void handleNotificationRequest_invalidRequestBody() {
        // when & then
        webTestClient.post()
                .uri("/api/v1/notifications")
                .header(MyHttpHeaders.IDEMPOTENCY_KEY, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    private NotificationRequestCommand createMockCommand() {
        return new NotificationRequestCommand(
                new NotificationRequestCommand.RequesterCommand(RequesterType.USER, "user-123"),
                new NotificationRequestCommand.RecipientsCommand(
                        List.of("user-456"), null, null, null),
                List.of(NotificationType.PUSH),
                Map.of(NotificationType.PUSH, new NotificationRequestCommand.SenderInfoCommand(null, null, "TestApp")),
                new NotificationRequestCommand.ContentCommand("Test Title", "Test Body", null, null),
                null,
                null,
                "Test memo");
    }

    private String createTestRequest() {
        return """
                {
                    "requester": {
                        "type": "USER",
                        "id": "user-123"
                    },
                    "recipients": {
                        "userIds": ["user-456"]
                    },
                    "notificationTypes": ["PUSH"],
                    "senderInfos": {
                        "PUSH": {
                            "senderName": "TestApp"
                        }
                    },
                    "content": {
                        "title": "Test Title",
                        "body": "Test Body"
                    },
                    "memo": "Test memo"
                }
                """;
    }
}
