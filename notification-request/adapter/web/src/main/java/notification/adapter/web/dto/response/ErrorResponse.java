package notification.adapter.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String timestamp, // 에러 발생 시각
        int status, // HTTP 상태 코드 (예: 400, 500)
        String error, // HTTP 상태 메시지 (예: Bad Request, Internal Server Error)
        String message, // 개발자가 정의한 상세 에러 메시지
        String path, // 요청된 경로 (예: /api/notifications/register)
        String code, // 특정 에러를 식별하기 위한 내부 에러 코드
        List<ValidationError> errors // 유효성 검사 오류 목록 등 상세 오류
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(LocalDateTime.now().toString(), status, error, message, path, null, null);
    }

    public ErrorResponse(int status, String error, String message, String path,
            String code, List<ValidationError> errors) {
        this(LocalDateTime.now().toString(), status, error, message, path, code, errors);
    }

    // 유효성 검사 오류를 위한 내부 레코드 (예: @Valid 실패 시)
    public record ValidationError(String field, String defaultMessage) {
    }
}