package notification.application.template.port.inbound;

import notification.definition.enums.NotificationType;
import notification.domain.vo.RenderedContent;
import notification.domain.vo.TemplateInfo;
import reactor.core.publisher.Mono;

public interface TemplateRenderingUseCase {

    Mono<RenderedContent> renderTemplate(TemplateInfo templateInfo, NotificationType type, String language);

}
