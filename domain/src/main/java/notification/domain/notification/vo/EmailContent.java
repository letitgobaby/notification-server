package notification.domain.notification.vo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import notification.domain.common.annotations.ValueObject;
import notification.domain.common.exceptions.DomainValidationException;

@ValueObject
public record EmailContent(
        String templateId, // 이메일 템플릿 ID (선택 사항)
        Map<String, Object> templateVariables, // 템플릿 변수 (선택 사항)
        String subject, // 제목 (템플릿 미사용 시 직접 지정)
        String htmlBody, // HTML 형식 본문 (템플릿 미사용 시 직접 지정)
        String textBody, // 텍스트 형식 본문 (HTML 미지원 클라이언트용, 템플릿 미사용 시 직접 지정)
        List<EmailAttachment> attachments // 첨부 파일 (optional)
) implements NotificationContent {

    public EmailContent {
        // 템플릿 ID와 제목/본문 중 최소 하나는 있어야 합니다.
        boolean hasTemplate = templateId != null && !templateId.isBlank();
        boolean hasDirectSubject = subject != null && !subject.isBlank();
        boolean hasDirectBody = (htmlBody != null && !htmlBody.isBlank()) || (textBody != null && !textBody.isBlank());

        // 유효성 검사: 템플릿 ID가 없고 제목과 본문이 모두 없는 경우 예외 발생
        if (!hasTemplate && (!hasDirectSubject || !hasDirectBody)) {
            throw new DomainValidationException("Email content must have either a template ID or a subject and body.");
        }

        // 유효성 검사: 템플릿 ID가 있는 경우 템플릿 변수가 null일 수 없음
        if (hasTemplate && templateVariables == null) {
            throw new DomainValidationException("Template variables cannot be null if a template ID is provided.");
        }

        templateVariables = templateVariables != null ? Collections.unmodifiableMap(templateVariables)
                : Collections.emptyMap();
        attachments = attachments != null ? processAttachments(attachments) : Collections.emptyList();
    }

    private List<EmailAttachment> processAttachments(List<EmailAttachment> attachments) {
        return attachments.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String toReadableString() {
        if (templateId != null && !templateId.isBlank()) {
            return "Email (Template: " + templateId + ")";
        }
        return "Email (Direct): " + subject;
    }

}