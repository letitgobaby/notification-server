package notification.definition.vo;

import java.util.Map;

import notification.definition.annotations.ValueObject;

/**
 * 알림 요청 처리를 위해 수집된 데이터를 담는 클래스
 * 
 * @param userConfigs       사용자 설정 정보 맵
 * @param renderedTemplates 렌더링된 템플릿 맵
 */
@ValueObject
public record CollectedData<T>(
        Map<String, T> userConfigs,
        Map<String, RenderedContent> renderedTemplates) {
}
