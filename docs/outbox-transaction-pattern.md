## Outbox Transaction Pattern을 사용한 메시지 수신

### # 배경

- 알림 요청 처리 시 데이터베이스에는 정상적으로 저장되었지만, 메시지 브로커(Kafka 등)로의 전송이 실패하면 알림 누락이 발생할 수 있습니다.
이를 방지하기 위해 요청 데이터와 Outbox 메시지를 동일한 트랜잭션 내에서 처리하여 데이터의 일관성을 보장하고, 전송 실패 시 롤백이 가능하도록 설계합니다.

### # 해결 방안

- [NotificationRequestService.java](../notification-request/application/src/main/java/notification/application/service/NotificationRequestService.java)
    - 요청을 바로 처리하지 않고 먼저 영속화 시키는 로직으로, Outbox Transaction Pattern을 적용하여 메시지 전송을 관리합니다.

- [RequestOutboxPollingService.java](../notification-request/application/src/main/java/notification/application/service/RequestOutboxPollingService.java)
    - RequestOutbox 테이블을 폴링하여 메시지를 전송하는 서비스로, 스케줄러를 통해 주기적으로 실행됩니다.

- [MessageOutboxPollingService.java](../notification-request/application/src/main/java/notification/application/service/MessageOutboxPollingService.java)
    - MessageOutbox 테이블을 폴링하여 메시지를 전송하는 서비스로, 스케줄러를 통해 주기적으로 실행됩니다.

- [NotificationMessageExceptionHandler.java](../notification-request/application/src/main/java/notification/application/exception/NotificationMessageExceptionHandler.java)
    - 메시지 전송 중 발생하는 예외를 처리하는 핸들러로, 재시도 로직을 포함합니다.

- [NotificationRequestExecutionHandler.java](../notification-request/application/src/main/java/notification/application/handler/NotificationRequestExecutionHandler.java)
    - 알림 요청 파싱 중 발생하는 예외를 처리하는 핸들러로, 재시도 로직을 포함합니다.

<br>

~~~java
private Mono<RequestOutbox> unitOfWork(NotificationRequestCommand command) {
    return unitOfWorkExecutor.execute(
        doTransactionalOutboxFlow(command), // Business Logic
        requestOutboxEventPublisher::publish // After-Commit
    );
}
~~~

<br>

- Outbox Transaction Pattern을 사용하여 DB 저장과 MQ 전송을 분리하되, 트랜잭션 내에 메시지를 Outbox 테이블에 먼저 기록합니다.
    이후 스케줄러 또는 백그라운드 작업이 Outbox 테이블을 주기적으로 조회(polling) 하여 MQ로 메시지를 전달합니다.

- 메시지 전송 실패 시를 대비해 재시도 가능한 구조로 설계하고, 짧은 주기의 스케줄링을 통해 실시간성을 최대한 확보합니다.
    트랜잭션 종료 후에는 After Commit 로직을 통해 전송 작업을 처리합니다.

- 메세지 요청에는 scheduledAt 필드를 포함하여, 특정 시간에 전송이 필요한 경우를 처리할 수 있습니다. 
    이 필드는 원본 데이터와 Outbox 테이블에 저장되며, 스케줄러는 이 필드를 기준으로 메시지를 전송합니다.
    scheduledAt 필드가 없는 경우, ApplicationEventPublisher를 사용하여 즉시 전송을 처리합니다. 
    scheduledAt 필드가 있는 경우, 스케줄링을 통해 해당 시간에 맞춰 메시지를 전송합니다.

- ApplicationEventPublisher와 Outbox Pattern을 함께 사용함으로써,
    **실시간 전송(즉시 처리)** 과 **예약 전송(지연 처리)** 을 모두 유연하게 처리할 수 있습니다.
    이 접근은 Outbox 기반 스케줄링 구조의 실시간성 부족 문제를 일부 해소하며,
    동시에 데이터 일관성과 전송 신뢰성을 확보하는 데 유리합니다.

<br>

### # 한계

- Outbox 테이블이 지속적으로 증가할 수 있어 Outbox 저장소의 **백그라운드 정리 작업**이 필요합니다.

- 전송 실패 시 재시도 로직이 필수적이며, 실패 메시지 처리 및 Dead Letter Queue에 대한 별도 관리가 필요합니다.

- Outbox 테이블을 폴링하는 구조는 실시간성이 떨어지며, 폴링 주기 조정에 따라 부하나 지연이 발생할 수 있습니다.

- 다중 인스턴스 환경에서 스케줄러 충돌(중복 전송)을 방지하기 위한 Lock 처리 전략이 필요합니다.

<br>

### # 다른 고려 사항

- CDC(Change Data Capture) 기반 처리

    - Database 테이블의 변경 사항을 실시간으로 감지하여 메시지를 전송하는 방법입니다. 
    
    - Outbox 테이블의 변경 내용을 Debezium과 같은 CDC 도구로 감지하여 MQ에 전송하는 방식. 실시간성이 향상되며, Application의 스케줄링 부하가 줄어듭니다. 대신, Kafka Connect 등의 외부 시스템이 필요하므로 인프라 복잡도가 증가합니다.
