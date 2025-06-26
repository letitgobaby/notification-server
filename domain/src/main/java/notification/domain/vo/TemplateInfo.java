package notification.domain.vo;

import java.util.Collections;
import java.util.Map;

import notification.definition.annotations.ValueObject;
import notification.definition.exceptions.MandatoryFieldException;

@ValueObject
public record TemplateInfo(
        TemplateId templateId,
        Map<String, String> templateParameters) {

    public TemplateInfo {
        if (templateId == null) {
            throw new MandatoryFieldException("Template ID cannot be null");
        }

        templateParameters = Collections.unmodifiableMap(templateParameters != null ? templateParameters : Map.of());
    }
}
