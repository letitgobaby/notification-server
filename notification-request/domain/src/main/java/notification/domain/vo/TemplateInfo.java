package notification.domain.vo;

import java.io.Serializable;
import java.util.Map;

import lombok.Getter;

@Getter
public class TemplateInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String templateInfoId;
    private final String templateId;
    private final Map<String, String> parameters;

    public TemplateInfo(String templateInfoId, String templateId, Map<String, String> parameters) {
        this.templateInfoId = templateInfoId;
        this.templateId = templateId;
        this.parameters = parameters;
    }

    public TemplateInfo(String templateId, Map<String, String> parameters) {
        this.templateInfoId = null; // This will be set later when saved to the database
        this.templateId = templateId;
        this.parameters = parameters;
    }

}