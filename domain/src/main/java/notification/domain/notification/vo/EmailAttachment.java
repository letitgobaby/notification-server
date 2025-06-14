package notification.domain.notification.vo;

import notification.domain.common.annotations.ValueObject;
import notification.domain.common.exceptions.DomainValidationException;

@ValueObject
public record EmailAttachment(
        String fileName, // 파일 이름
        String filePath, // 파일 경로 (로컬 또는 URL)
        String contentType // MIME 타입 (예: "image/png", "application/pdf" 등)
) {

    public EmailAttachment {
        if (fileName == null || fileName.isBlank()) {
            throw new DomainValidationException("File name cannot be null or empty");
        }

        if (filePath == null || filePath.isBlank()) {
            throw new DomainValidationException("File path cannot be null or empty");
        }

        if (contentType == null || contentType.isBlank()) {
            throw new DomainValidationException("Content type cannot be null or empty");
        }
    }

}
