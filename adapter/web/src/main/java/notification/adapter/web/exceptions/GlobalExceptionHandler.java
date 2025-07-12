package notification.adapter.web.exceptions;

import java.util.stream.Collectors;

import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.adapter.web.dto.response.ErrorResponse;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        log.error("Validation error at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                "Validation failed: " + ex.getReason(),
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServerWebInputException(
            ServerWebInputException ex, ServerWebExchange exchange) {
        log.error("Input validation error at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                "Invalid input: " + ex.getReason(),
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResponseStatusException(
            ResponseStatusException ex, ServerWebExchange exchange) {
        log.error("Response status exception at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                ex.getReason() != null ? ex.getReason() : "Request failed",
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.status(status).body(errorResponse));
    }

    @ExceptionHandler(DecodingException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDecodingException(
            DecodingException ex, ServerWebExchange exchange) {
        log.error("JSON decoding error at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                "Invalid JSON format",
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(BindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBindException(
            BindException ex, ServerWebExchange exchange) {
        log.error("Bind exception at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        String errorMessage = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                "Validation failed: " + errorMessage,
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                "Internal server error",
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

}
