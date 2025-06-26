package notification.application.template.port.outbound;

import notification.application.template.dto.TemplateDefinition;
import reactor.core.publisher.Mono;

public interface TemplateDefinitionProviderPort {
    /**
     * Retrieves a template definition by its ID and language.
     *
     * @param id       the ID of the template
     * @param language the language of the template
     * @return a Mono containing the TemplateDefinition, or empty if not found
     */
    Mono<TemplateDefinition> getTemplateDefinition(String id, String type, String language);

}
