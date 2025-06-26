package notification.domain.vo;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.MandatoryFieldException;

@ValueObject
public record TemplateId(String value) {
    public TemplateId {
        if (value == null || value.isBlank()) {
            throw new MandatoryFieldException("TemplateId cannot be null or blank");
        }
    }

}
