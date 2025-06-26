package notification.application.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.template.port.inbound.TemplateRenderingUseCase;
import notification.application.template.port.outbound.TemplateDefinitionProviderPort;
import notification.definition.enums.NotificationType;
import notification.domain.vo.RenderedContent;
import notification.domain.vo.TemplateInfo;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateRenderingService implements TemplateRenderingUseCase {

    private final TemplateDefinitionProviderPort templateDefinitionProvider;

    /**
     * 템플릿을 렌더링합니다.
     * 
     * @param templateInfo 템플릿 정보
     * @param type         알림 타입
     * @param language     언어 코드
     * @return 렌더링된 콘텐츠
     */
    @Override
    public Mono<RenderedContent> renderTemplate(TemplateInfo templateInfo, NotificationType type, String language) {
        log.debug("Rendering template: {}, type : {}, language: {}",
                templateInfo.templateId(), type.name(), language);

        String templateId = templateInfo.templateId().value();
        return templateDefinitionProvider.getTemplateDefinition(templateId, type.name(), language)
                .flatMap(definition -> {
                    return Mono.just(new RenderedContent(
                            bindParameters(definition.titleTemplate(), templateInfo.templateParameters()),
                            bindParameters(definition.bodyTemplate(), templateInfo.templateParameters()),
                            definition.language(),
                            null));
                }).onErrorResume(e -> {
                    log.error("Failed to render template: {}, language: {}, error: {}", templateId, language,
                            e.getMessage());
                    return Mono.error(new RuntimeException("Template rendering failed", e));
                });
    }

    /**
     * 주어진 템플릿 문자열의 플레이스홀더를 파라미터 맵의 값으로 바인딩합니다.
     * 예: "안녕하세요. {{name}}님." + {"name": "홍길동"} -> "안녕하세요. 홍길동님."
     * 
     * @param templateString 템플릿 문자열
     * @param parameters     플레이스홀더와 대응되는 값의 맵
     * @return 바인딩된 문자열
     */
    private String bindParameters(String templateString, Map<String, String> parameters) {
        if (templateString == null || parameters == null || parameters.isEmpty()) {
            return templateString;
        }

        // {{var}} 형태의 플레이스홀더를 찾는 정규 표현식
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = pattern.matcher(templateString);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1); // {{name}} 에서 "name" 추출
            String replacement = parameters.getOrDefault(varName, ""); // 파라미터 맵에서 값 가져오기, 없으면 빈 문자열

            // replaceAll()은 문자열이 아닌 정규식 패턴을 받기 때문에
            // Literal Replacement를 위해 Matcher.appendReplacement/appendTail 사용
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
