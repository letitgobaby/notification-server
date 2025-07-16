package notification.adapter.client;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.application.template.port.outbound.TemplateDefinitionProviderPort;
import notification.definition.vo.TemplateDefinition;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockingTemplateDefinitionProviderAdapter implements TemplateDefinitionProviderPort {

    private ConcurrentHashMap<String, TemplateDefinition> templateDefinitions = new ConcurrentHashMap<>() {
        {
            put("NEW_PRODUCT_LAUNCH", new TemplateDefinition(
                    "NEW_PRODUCT_LAUNCH",
                    "ko",
                    "[신제품 출시] ${productName}",
                    "📣 ${productName}가 ${launchDate}에 출시됩니다. 많은 관심 부탁드립니다!"));

            put("ORDER_SHIPPED", new TemplateDefinition(
                    "ORDER_SHIPPED",
                    "ko",
                    "주문이 발송되었습니다: ${productName}",
                    "주문하신 ${productName} 상품이 ${shippingCompany}를 통해 발송되었습니다. 운송장 번호는 ${trackingNumber}입니다."));

            put("VIP_DISCOUNT_EMAIL", new TemplateDefinition(
                    "VIP_DISCOUNT_EMAIL",
                    "ko",
                    "[VIP 전용 혜택]",
                    "VIP 고객님께 드리는 ${discountRate} 할인 혜택! 지금 ${couponCode} 쿠폰을 사용해보세요."));
        }
    };

    @Override
    public Mono<TemplateDefinition> getTemplateDefinition(String id, String type, String language) {
        if (id == null || !templateDefinitions.containsKey(id)) {
            log.warn("Template definition not found for id: {}, type: {}, language: {}", id, type, language);
            return Mono.empty();
        }

        TemplateDefinition templateDefinition = templateDefinitions.get(id);
        if (templateDefinition == null || !templateDefinition.language().equals(language)) {
            log.warn("Template definition not found for id: {}, type: {}, language: {}", id, type, language);
            return Mono.empty();
        }

        return Mono.just(templateDefinition);
    }

}
