package notification.domain.notification.vo;

import java.util.Collections;
import java.util.List;

import notification.domain.common.annotations.ValueObject;
import notification.domain.common.exceptions.DomainValidationException;

@ValueObject
public record PushRecipient(List<String> deviceTokens) implements Recipient {

    public PushRecipient {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            throw new DomainValidationException("Device tokens cannot be null or empty");
        }

        deviceTokens = Collections.unmodifiableList(deviceTokens);
    }

    @Override
    public String toReadableString() {
        return "Push to devices: " + String.join(", ", deviceTokens);
    }

}