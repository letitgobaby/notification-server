package notification.definition.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 멱등성 작업을 나타내는 어노테이션입니다.
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
