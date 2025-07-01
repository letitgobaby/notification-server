package notification.application.template.port.inbound;

import notification.definition.vo.RenderedContent;
import notification.domain.enums.NotificationType;
import notification.domain.vo.TemplateInfo;
import reactor.core.publisher.Mono;

public interface TemplateRenderingUseCase {

    Mono<RenderedContent> renderTemplate(TemplateInfo templateInfo, NotificationType type, String language);

}
