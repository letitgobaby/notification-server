package notification.definition.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 어노테이션은 메서드가 멱등성을 보장해야 함을 나타냅니다.
 * 이 어노테이션이 붙은 메서드는 멱등성 Aspect에 의해 가로채져
 * 중복 요청이 실제 비즈니스 로직을 여러 번 실행하는 것을 방지합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 멱등성 키를 생성하기 위한 인자 이름입니다.
     * 이 인자는 멱등성 키를 생성하는 데 사용됩니다.
     * 예를 들어, "userId" 또는 "requestId"와 같은 값을 사용할 수 있습니다.
     */
    String argKey();

    /**
     * 이 멱등성 작업의 타입을 지정합니다 (예: "ORDER_CREATION", "PAYMENT_PROCESSING").
     * 같은 멱등성 키라도 작업 타입이 다르면 별개의 멱등성으로 간주될 수 있습니다.
     * 기본값은 어노테이션이 적용된 메서드의 이름입니다.
     */
    String operationType() default "";

}
