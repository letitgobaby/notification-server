package notification.domain.notification.vo;

import java.util.Collections;
import java.util.List;

import notification.domain.common.annotations.ValueObject;
import notification.domain.common.exceptions.DomainValidationException;

@ValueObject
public record SmsRecipient(
        List<String> phoneNumbers,
        String callbackNumber // 콜백 번호 (선택 사항, 기본값은 null)
) implements Recipient {

    public SmsRecipient {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            throw new DomainValidationException("Phone numbers cannot be null or empty");
        }

        phoneNumbers = Collections.unmodifiableList(phoneNumbers);
    }

    @Override
    public String toReadableString() {
        return "SMS to: " + String.join(", ", phoneNumbers);
    }

}