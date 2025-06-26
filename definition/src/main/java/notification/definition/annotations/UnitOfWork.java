package notification.definition.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 어노테이션이 적용된 메서드는 트랜잭션을 시작하고, 메서드 실행이 완료되면 커밋하거나 롤백합니다.
 * 실제 트랜잭션 처리는 Adapter에서 AOP Aspect에서 구현되어야 합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UnitOfWork {

}
