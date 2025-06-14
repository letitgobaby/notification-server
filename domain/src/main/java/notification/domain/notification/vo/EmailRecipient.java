package notification.domain.notification.vo;

import java.util.Collections;
import java.util.List;

import notification.domain.common.annotations.ValueObject;
import notification.domain.common.exceptions.DomainValidationException;

@ValueObject
public record EmailRecipient(
        List<String> toAddresses, // 수신자 이메일 주소 목록
        List<String> ccAddresses, // 참조 이메일 주소 목록
        List<String> bccAddresses // 숨은 참조 이메일 주소 목록
) implements Recipient {

    public EmailRecipient {
        if (toAddresses == null || toAddresses.isEmpty()) {
            throw new DomainValidationException("To addresses cannot be null or empty");
        }

        toAddresses = Collections.unmodifiableList(toAddresses);
        ccAddresses = ccAddresses == null ? List.of() : Collections.unmodifiableList(ccAddresses);
        bccAddresses = bccAddresses == null ? List.of() : Collections.unmodifiableList(bccAddresses);
    }

    @Override
    public String toReadableString() {
        return "Email to: " + String.join(", ", toAddresses) +
                (ccAddresses.isEmpty() ? "" : ", CC: " + String.join(", ", ccAddresses)) +
                (bccAddresses.isEmpty() ? "" : ", BCC: " + String.join(", ", bccAddresses));
    }

}
