package notification.adapter.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.adapter.web.dto.response.ErrorResponse;
import notification.definition.exceptions.BusinessRuleViolationException;
import notification.definition.exceptions.DataNotFoundException;
import notification.definition.exceptions.DuplicateRequestException;
import notification.definition.exceptions.MandatoryFieldException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApplicationExceptionHandler {

    @ExceptionHandler(DataNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDataNotFoundException(
            DataNotFoundException ex, ServerWebExchange exchange) {

        log.error("Data not found at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                "Data not found",
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicateRequestException(
            DuplicateRequestException ex, ServerWebExchange exchange) {
        log.error("Duplicate request at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                "Duplicate request",
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse));
    }

    @ExceptionHandler(MandatoryFieldException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleMandatoryFieldException(
            MandatoryFieldException ex, ServerWebExchange exchange) {
        log.error("Mandatory field missing at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                "Mandatory field missing",
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessRuleViolationException(
            BusinessRuleViolationException ex, ServerWebExchange exchange) {
        log.error("Business rule violation at [{} {}]: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getMessage(), ex);

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), status.getReasonPhrase(),
                "Business rule violation",
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

}
