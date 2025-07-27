## 다중 인스턴스 환경에서 스케줄링 시 Outbox 중복 조회 방지 전략 - Select Locking Strategy

### # 이슈 발생
~~~java
    @Scheduled(fixedDelayString = "${app.outbox.polling-interval-ms:5000}")
    public void poll() {
        log.info("Starting RequestOutbox polling...");
        requestOutboxPollingService.poll().subscribe(); // 비동기적으로 실행
    }
~~~

- 처리량을 늘리기 위해 Scheduler Lock 기능을 사용하지 않고, 각 인스턴스가 독립적으로 Outbox 데이터를 조회하고 처리하는 방식으로 구현 해야 함. 이 경우, 다중 인스턴스 환경에서 Outbox 데이터를 조회할 때, 각 인스턴스가 동일한 Outbox 데이터를 동시에 조회하여 중복 처리되는 상황이 발생함

- MariaDB 10.6.4 버전부터 `FOR UPDATE SKIP LOCKED` 기능이 지원되지만, 이 기능을 사용하지 않고 Outbox Locking Strategy를 구현해야 하는 상황.

### # 해결 방안

- [RequestOutboxRepositoryAdapter.java](../notification-request/adapter/db/src/main/java/notification/adapter/db/adapter/RequestOutboxRepositoryAdapter.java)
- [MessageOutboxRepositoryAdapter.java](../notification-request/adapter/db/src/main/java/notification/adapter/db/adapter/MessageOutboxRepositoryAdapter.java)

```java
// Outbox 데이터를 선점하고, 해당 Outbox 데이터를 조회하는 방식으로 구현
public Flux<RequestOutbox> fetchOutboxToProcess(Instant now, int limit) {
    // 인스턴스 ID 생성 (UUID 사용)
    String instanceId = UUID.randomUUID().toString(); 

    return transactionalOperator.transactional(
        // 선점 쿼리 실행 +  REQUIRES_NEW 트랜잭션으로 commit
        updateOutboxForLock(now, limit, instanceId) 
    ).thenMany(Flux.defer(() -> {
        // 선점된 Outbox 데이터 조회
        return selectLockedOutbox(instanceId, limit); 
    }));
}
```

```sql
-- Update 쿼리
UPDATE request_outbox
SET instance_id = :instanceId, status = :status
WHERE instance_id IS NULL
    AND outbox_id IN (
        SELECT outbox_id FROM (
            SELECT outbox_id FROM request_outbox
            WHERE status IN ('PENDING', 'FAILED')
                AND (next_retry_at IS NULL OR next_retry_at <= NOW())
                AND instance_id IS NULL
            ORDER BY created_at ASC
            LIMIT %d
        ) AS subquery
    );
```

- Outbox 조회시 선점 쿼리를 먼저 실행하여, 해당 Outbox 데이터를 다른 인스턴스가 처리하지 못하도록 lock을 걸고, 이후 선점된 데이터를 조회하는 방식으로 구현.

- 선점 쿼리는 **독립적인 트랜잭션**으로 실행되어야 하며, 외부 트랜잭션의 영향을 받지 않고 즉시 commit되도록 처리.

- 인스턴스 ID를 이용하여 선점된 Outbox 데이터를 조회하며, 이를 통해 중복 조회를 방지.


<br/>

### # 한계

- Update, Select 두 번의 쿼리를 실행해야 하므로, Outbox 데이터가 많을 경우 성능 저하가 발생할 수 있음. 특히, 선점 쿼리에서 서브쿼리를 사용하여 Outbox 데이터를 조회하는 부분이 성능에 영향을 줄 수 있음.

- 선점 쿼리에서 서브쿼리를 사용하여 Outbox 데이터를 조회하는 부분이 성능에 영향을 줄 수 있음. 따라서, Outbox 데이터의 양이 많아질 경우, 선점 쿼리의 성능을 최적화하거나, Outbox 데이터를 분할하여 처리하는 등의 추가적인 최적화가 필요할 수 있음.

### # 다른 고려 사항

1. SELECT ... FOR UPDATE SKIP LOCKED 

    - PostgreSQL, Oracle, MySQL 8.0 이상 지원되는 기능으로, Outbox 데이터를 조회할 때 다른 트랜잭션이 해당 데이터를 수정하지 못하도록 lock을 걸 수 있음.

    - 포트 어댑터 에서 `FOR UPDATE SKIP LOCKED` 기능을 사용하여 Outbox 데이터를 조회할 수 있지만, 이 기능을 사용하지 않고 Outbox Locking Strategy를 구현해야 하는 상황에서는 위와 같은 방식으로 선점 쿼리를 작성해야 함.


2. Redis 기반 분산 락

    - Redis를 이용하여 분산 락을 구현할 수 있음. 각 인스턴스가 Outbox 데이터를 조회하기 전에 Redis에 lock을 획득하고, lock을 획득한 인스턴스만 Outbox 데이터를 조회하도록 구현할 수 있음.

    - 이 경우, Redis의 TTL(Time To Live)을 설정하여 lock이 일정 시간 후 자동으로 해제되도록 처리해야 함. 이를 통해 인스턴스가 비정상 종료되더라도 lock이 해제되어 다른 인스턴스가 Outbox 데이터를 처리할 수 있도록 해야 함.

    - Redis 기반 분산 락은 외부 서버의 의존성이 추가되므로, 시스템의 복잡성이 증가할 수 있음. 또한, 분산 락의 경우 대량의 데이터를 처리할 때 lock을 획득하는 과정에서 성능 저하가 발생할 수 있음. 따라서, Outbox Locking Strategy를 구현할 때는 Redis 기반 분산 락을 사용하는 것보다 선점 쿼리를 이용한 방식이 더 적합할 수 있음.
