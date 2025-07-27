package notification.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.adapter.web.constants.MyHttpHeaders;
import notification.adapter.web.dto.request.NotificationCreateRequest;
import notification.adapter.web.dto.response.NotificationRequestResponse;
import notification.adapter.web.mapper.NotificationCreateRequestMapper;
import notification.application.notifiation.port.inbound.ProcessNotificationRequestUseCase;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationRequestController {

    private final ProcessNotificationRequestUseCase processNotificationRequest;
    private final NotificationCreateRequestMapper notificationCreateRequestMapper;

    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<NotificationRequestResponse> handleNotificationRequest(
            @NotEmpty @RequestHeader(name = MyHttpHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody NotificationCreateRequest request) {
        log.info("Received notification request: [{}] {}", idempotencyKey, request);

        return Mono.fromCallable(() -> notificationCreateRequestMapper.toCommand(request))
                .flatMap(command -> processNotificationRequest.handle(command, idempotencyKey))
                .map(result -> new NotificationRequestResponse(
                        result.notificationId(), result.status(), result.message()));
    }

}
