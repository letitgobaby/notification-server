## 멱등성 키를 사용하여 중복 요청 방지

### # 이슈 발생

- 사용자의 요청이 네트워크 문제 등으로 인해 중복으로 처리되는 경우가 발생할 수 있고, 이로 인해 동일한 알림이 여러 번 발송되는 문제가 발생할 수 있습니다.

<br>

### # 해결 방안

- [Idempotent.java](../notification-request/definition/src/main/java/notification/definition/Idempotent.java)
- [IdempotentOperationAspect.java](../notification-request/infrastructure/src/main/java/notification/infrastructure/aop/IdempotentOperationAspect.java)
- [IdempotentOperationService.java](../notification-request/application/src/main/java/notification/application/service/IdempotentOperationService.java)

<br>

- 멱등성 키를 사용하여 중복 요청을 방지합니다. 클라이언트는 요청 시 HTTP 해더에 멱등성 키를 포함하고, 서버는 이 키를 사용하여 이미 처리된 요청인지 확인합니다.

<br>

~~~java
// 멱등성 키를 정의하는 커스텀 어노테이션
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String argKey();
    String operationType() default "";
}

// 멱등성 키를 사용하여 중복 요청을 방지하는 예시
@Idempotent(argKey = "idempotencyKey", operationType = "NOTIFICATION_REQUEST")
public Mono<NotificationRequestResult> handle(NotificationRequestCommand command, String idempotencyKey) {
    ...
}
~~~

- 멱등성 키를 이용한 중복 방지 로직은 Spring AOP를 사용하여 구현하였습니다. AOP를 사용하여 비즈니스 로직과 분리하여 유지보수성을 높였습니다.

- 이미 처리된 요청인 경우, 서버는 해당 요청에 대한 응답을 캐싱 데이터에서 확인하여 반환하고, 새로운 요청으로 간주하지 않습니다.

<br>

### # 한계

- 멱등성 키를 사용하여 중복 요청을 방지하는 방식은 클라이언트가 멱등성 키를 제공해야 하므로, 클라이언트와 서버 간의 협약이 필요합니다. 클라이언트가 멱등성 키를 제공하지 않거나, 동일한 키를 다른 API에 사용하는 경우 중복 요청이 발생할 수 있습니다.

- 멱등성 키를 사용하여 중복 요청을 방지하는 방식은 서버 측에서 멱등성 키를 관리해야 하므로, 추가적인 저장소나 캐시가 필요할 수 있습니다. 이로 인해 시스템의 복잡성이 증가할 수 있습니다.

- 이 프로젝트에서는 멱등성 키를 DB 테이블로 관리하고 있습니다. 이로 인해 멱등성 키의 유효 기간을 설정하거나, 키의 만료 처리를 추가로 구현해야 할 수 있습니다. TTL 설정을 위해 Redis와 같은 캐시 시스템을 사용할 경우 외부 시스템에 의존하게 되어 시스템의 복잡성이 증가할 수 있습니다.

<br>

### # 다른 고려 사항

- **Redis 기반 캐시**
    - 멱등성 키를 Redis와 같은 캐시 시스템에 저장하여, 빠른 조회와 만료 처리를 할 수 있습니다. Redis는 TTL(Time To Live) 기능을 제공하므로, 멱등성 키의 유효 기간을 설정할 수 있습니다.

- **API Gateway 선처리**
    - API Gateway에서 멱등성 키를 미리 검사하고, 이미 처리된 요청인 경우 해당 요청을 차단하여 내부 서비스의 부하를 감소시킬 수 있습니다. 이를 통해 중복 요청이 서버에 도달하기 전에 사전에 방지할 수 있습니다.

