package notification.adapter.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import notification.adapter.web.constants.MyHttpHeaders;
import notification.adapter.web.dto.request.NotificationCreateRequest;
import notification.adapter.web.exceptions.ApplicationExceptionHandler;
import notification.adapter.web.exceptions.GlobalExceptionHandler;
import notification.adapter.web.mapper.NotificationCreateRequestMapper;
import notification.application.notifiation.dto.NotificationRequestCommand;
import notification.application.notifiation.dto.NotificationRequestResult;
import notification.application.notifiation.port.inbound.NotificationRequestReceviedUseCase;
import notification.definition.exceptions.BusinessRuleViolationException;
import notification.definition.exceptions.DataNotFoundException;
import notification.definition.exceptions.DuplicateRequestException;
import notification.definition.exceptions.MandatoryFieldException;
import notification.domain.enums.NotificationType;
import notification.domain.enums.RequesterType;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRequestController 통합 테스트")
class NotificationRequestControllerIntegrationTest {

    private WebTestClient webTestClient;

    @Mock
    private NotificationRequestReceviedUseCase notificationRequestReceviedService;

    @Mock
    private NotificationCreateRequestMapper notificationCreateRequestMapper;

    private final String validIdempotencyKey = "test-idempotency-key-123";
    private NotificationRequestCommand mockCommand;
    private NotificationRequestResult successResult;

    @BeforeEach
    void setUp() {
        mockCommand = createMockCommand();
        successResult = NotificationRequestResult.success("notification-123");

        // WebTestClient 설정
        NotificationRequestController controller = new NotificationRequestController(
                notificationRequestReceviedService, notificationCreateRequestMapper);

        ApplicationExceptionHandler exceptionHandler = new ApplicationExceptionHandler();
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(exceptionHandler)
                .controllerAdvice(globalExceptionHandler)
                .build();
    }

    @Nested
    @DisplayName("성공 케이스 테스트")
    class SuccessTest {

        @Test
        @DisplayName("유효한 요청으로 알림 요청 성공")
        void handleNotificationRequest_withValidRequest_success() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.just(successResult));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createValidRequestJson())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.notificationId").isEqualTo("notification-123")
                    .jsonPath("$.status").isEqualTo("SUCCESS")
                    .jsonPath("$.message").isEqualTo("Notification request registered successfully.");
        }

        @Test
        @DisplayName("템플릿 기반 요청 성공")
        void handleNotificationRequest_withTemplateRequest_success() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.just(successResult));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createTemplateRequestJson())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.notificationId").isEqualTo("notification-123")
                    .jsonPath("$.status").isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("예약 발송 요청 성공")
        void handleNotificationRequest_withScheduledRequest_success() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.just(successResult));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createScheduledRequestJson())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.notificationId").isEqualTo("notification-123")
                    .jsonPath("$.status").isEqualTo("SUCCESS");
        }
    }

    @Nested
    @DisplayName("유효성 검증 실패 테스트")
    class ValidationFailureTest {

        @Test
        @DisplayName("Idempotency Key 누락 시 400 에러")
        void handleNotificationRequest_withoutIdempotencyKey_badRequest() {
            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createValidRequestJson())
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("빈 Idempotency Key 시 400 에러")
        void handleNotificationRequest_withEmptyIdempotencyKey_badRequest() {
            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, "")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createValidRequestJson())
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러")
        void handleNotificationRequest_withMissingRequiredFields_badRequest() {
            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("잘못된 JSON 형식 시 400 에러")
        void handleNotificationRequest_withInvalidJson_badRequest() {
            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("invalid json")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("requester 정보 누락 시 400 에러")
        void handleNotificationRequest_withoutRequester_badRequest() {
            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequestWithoutRequesterJson())
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("수신자 정보 누락 시 400 에러")
        void handleNotificationRequest_withoutRecipients_badRequest() {
            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequestWithoutRecipientsJson())
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("content와 template 둘 다 제공 시 400 에러")
        void handleNotificationRequest_withBothContentAndTemplate_badRequest() {
            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequestWithBothContentAndTemplateJson())
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("content와 template 둘 다 누락 시 400 에러")
        void handleNotificationRequest_withoutContentAndTemplate_badRequest() {
            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequestWithoutContentAndTemplateJson())
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("서비스 예외 처리 테스트 (Advice 동작 확인)")
    class ServiceExceptionTest {

        @Test
        @DisplayName("DataNotFoundException 발생 시 404 응답")
        void handleNotificationRequest_withDataNotFoundException_notFound() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.error(new DataNotFoundException("User not found")));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createValidRequestJson())
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.error").isEqualTo("Not Found")
                    .jsonPath("$.message").isEqualTo("Data not found")
                    .jsonPath("$.path").isEqualTo("/api/v1/notifications");
        }

        @Test
        @DisplayName("DuplicateRequestException 발생 시 409 응답")
        void handleNotificationRequest_withDuplicateRequestException_conflict() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.error(new DuplicateRequestException("Duplicate idempotency key")));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createValidRequestJson())
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(409)
                    .jsonPath("$.error").isEqualTo("Conflict")
                    .jsonPath("$.message").isEqualTo("Duplicate request")
                    .jsonPath("$.path").isEqualTo("/api/v1/notifications");
        }

        @Test
        @DisplayName("MandatoryFieldException 발생 시 400 응답")
        void handleNotificationRequest_withMandatoryFieldException_badRequest() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.error(new MandatoryFieldException("Required field missing")));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createValidRequestJson())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.error").isEqualTo("Bad Request")
                    .jsonPath("$.message").isEqualTo("Mandatory field missing")
                    .jsonPath("$.path").isEqualTo("/api/v1/notifications");
        }

        @Test
        @DisplayName("BusinessRuleViolationException 발생 시 400 응답")
        void handleNotificationRequest_withBusinessRuleViolationException_badRequest() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.error(new BusinessRuleViolationException("Business rule violated")));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createValidRequestJson())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.error").isEqualTo("Bad Request")
                    .jsonPath("$.message").isEqualTo("Business rule violation")
                    .jsonPath("$.path").isEqualTo("/api/v1/notifications");
        }

        @Test
        @DisplayName("일반 RuntimeException 발생 시 500 Internal Server Error 반환")
        void handleNotificationRequest_withRuntimeException_returns500() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createValidRequestJson())
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(500)
                    .jsonPath("$.error").isEqualTo("Internal Server Error")
                    .jsonPath("$.message").isEqualTo("Internal server error")
                    .jsonPath("$.path").isEqualTo("/api/v1/notifications");
        }

        @Test
        @DisplayName("Mapper에서 예외 발생 시 500 Internal Server Error 반환")
        void handleNotificationRequest_withMapperException_returns500() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenThrow(new RuntimeException("Mapping failed"));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createValidRequestJson())
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(500)
                    .jsonPath("$.error").isEqualTo("Internal Server Error")
                    .jsonPath("$.message").isEqualTo("Internal server error")
                    .jsonPath("$.path").isEqualTo("/api/v1/notifications");
        }
    }

    @Nested
    @DisplayName("다양한 요청 시나리오 테스트")
    class VariousRequestScenarioTest {

        @Test
        @DisplayName("여러 알림 타입 동시 요청")
        void handleNotificationRequest_withMultipleNotificationTypes_success() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.just(successResult));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createMultiTypeRequestJson())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.notificationId").isEqualTo("notification-123")
                    .jsonPath("$.status").isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("전체 사용자 대상 요청")
        void handleNotificationRequest_withAllUsersTarget_success() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.just(successResult));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createAllUsersRequestJson())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.notificationId").isEqualTo("notification-123")
                    .jsonPath("$.status").isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("직접 수신자 대상 요청")
        void handleNotificationRequest_withDirectRecipients_success() {
            // given
            when(notificationCreateRequestMapper.toCommand(any(NotificationCreateRequest.class)))
                    .thenReturn(mockCommand);
            when(notificationRequestReceviedService.handle(eq(mockCommand), eq(validIdempotencyKey)))
                    .thenReturn(Mono.just(successResult));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/notifications")
                    .header(MyHttpHeaders.IDEMPOTENCY_KEY, validIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createDirectRecipientsRequestJson())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.notificationId").isEqualTo("notification-123")
                    .jsonPath("$.status").isEqualTo("SUCCESS");
        }
    }

    // Helper methods for creating mock objects and JSON strings
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

    private String createValidRequestJson() {
        return """
                {
                    "requester": {
                        "type": "USER",
                        "id": "user-123"
                    },
                    "recipients": {
                        "userIds": ["user-456", "user-789"]
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

    private String createTemplateRequestJson() {
        return """
                {
                    "requester": {
                        "type": "USER",
                        "id": "user-123"
                    },
                    "recipients": {
                        "userIds": ["user-456"]
                    },
                    "notificationTypes": ["EMAIL"],
                    "senderInfos": {
                        "EMAIL": {
                            "senderEmailAddress": "sender@example.com",
                            "senderName": "Test Sender"
                        }
                    },
                    "template": {
                        "templateId": "WELCOME_TEMPLATE",
                        "templateParameters": {
                            "userName": "John Doe",
                            "productName": "Smart Watch"
                        }
                    }
                }
                """;
    }

    private String createScheduledRequestJson() {
        Instant futureTime = Instant.now().plusSeconds(3600);
        return String.format("""
                {
                    "requester": {
                        "type": "ADMIN",
                        "id": "admin-123"
                    },
                    "recipients": {
                        "userIds": ["user-456"]
                    },
                    "notificationTypes": ["SMS"],
                    "senderInfos": {
                        "SMS": {
                            "senderPhoneNumber": "02-1234-5678",
                            "senderName": "Test SMS"
                        }
                    },
                    "content": {
                        "title": "Scheduled Notification",
                        "body": "This is scheduled"
                    },
                    "scheduledAt": "%s",
                    "memo": "Scheduled notification test"
                }
                """, futureTime.toString());
    }

    private String createRequestWithoutRequesterJson() {
        return """
                {
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
                    }
                }
                """;
    }

    private String createRequestWithoutRecipientsJson() {
        return """
                {
                    "requester": {
                        "type": "USER",
                        "id": "user-123"
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
                    }
                }
                """;
    }

    private String createRequestWithBothContentAndTemplateJson() {
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
                    "template": {
                        "templateId": "TEST_TEMPLATE"
                    }
                }
                """;
    }

    private String createRequestWithoutContentAndTemplateJson() {
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
                    }
                }
                """;
    }

    private String createMultiTypeRequestJson() {
        return """
                {
                    "requester": {
                        "type": "USER",
                        "id": "user-123"
                    },
                    "recipients": {
                        "userIds": ["user-456"]
                    },
                    "notificationTypes": ["SMS", "EMAIL", "PUSH"],
                    "senderInfos": {
                        "SMS": {
                            "senderPhoneNumber": "02-1234-5678",
                            "senderName": "Test SMS"
                        },
                        "EMAIL": {
                            "senderEmailAddress": "sender@example.com",
                            "senderName": "Test Email"
                        },
                        "PUSH": {
                            "senderName": "Test Push"
                        }
                    },
                    "content": {
                        "title": "Multi Type Notification",
                        "body": "This supports multiple types"
                    }
                }
                """;
    }

    private String createAllUsersRequestJson() {
        return """
                {
                    "requester": {
                        "type": "ADMIN",
                        "id": "admin-123"
                    },
                    "recipients": {
                        "allUsers": true
                    },
                    "notificationTypes": ["PUSH"],
                    "senderInfos": {
                        "PUSH": {
                            "senderName": "System Notification"
                        }
                    },
                    "content": {
                        "title": "Important Announcement",
                        "body": "This is for all users"
                    }
                }
                """;
    }

    private String createDirectRecipientsRequestJson() {
        return """
                {
                    "requester": {
                        "type": "SERVICE",
                        "id": "service-123"
                    },
                    "recipients": {
                        "directRecipients": [
                            {
                                "phoneNumber": "01012345678",
                                "email": "test@example.com",
                                "deviceToken": "device-token-123"
                            },
                            {
                                "email": "test2@example.com"
                            }
                        ]
                    },
                    "notificationTypes": ["EMAIL"],
                    "senderInfos": {
                        "EMAIL": {
                            "senderEmailAddress": "service@example.com",
                            "senderName": "Service Team"
                        }
                    },
                    "content": {
                        "title": "Direct Notification",
                        "body": "This is sent directly"
                    }
                }
                """;
    }
}
