
## 📖 사용자 알림요청 처리 프로세스

알림 요청 API 프로세스는 다음과 같은 단계로 진행됩니다:

<br/>

### # 클라이언트 요청 프로세스

<br/>

1. **알림 요청 접수**

    - 클라이언트가 REST API를 통해 알림 요청을 생성합니다.

    - 요청은 JSON 형식으로 전달되며, 요청자, 수신자 목록, 알림 채널, 발신자 정보 등을 포함합니다.

    - [요청 샘플 데이터](./client/request.js)

<br/>

2. **요청 검증 및 NotificationRequest 도메인 객체 생성**

    - Web Adapter에서 요청을 검증하고 Application Service로 전달합니다.

    - NotificationRequest.create() 팩토리 메서드를 통해 도메인 객체를 생성합니다.

    - 필수 필드, 빈 컬렉션, 내용/템플릿 존재 여부 등 비즈니스 규칙을 검증합니다.

    - 초기 상태는 RequestStatus.PENDING으로 설정됩니다.

<br/>

3. **RequestOutbox 메세지 생성**

    - NotificationRequest 도메인 객체를 OutboxMessage로 변환합니다.

    - OutboxMessage는 알림 요청의 상태, Payload(요청 데이터), ScheduledAt(전송 예정 시간) 등을 포함합니다.

    - ScheduledAt 필드는 즉시 전송을 원할 경우 현재 시각으로 설정됩니다.


<br/>

4. **NotificationRequest & RequestOutbox 트렌젝션 저장**

    - NotificationRequest 도메인 객체와 RequestOutbox 메세지를 데이터베이스에 저장합니다.

    - 하나의 트랜잭션을 통해 두 객체를 동시에 저장하여 일관성을 유지합니다.

    - 이때, NotificationRequest, RequestOutbox의 상태는 PENDING으로 유지됩니다.

    - 저장이 완료되면 NotificationRequestReceivedEvent 이벤트가 발생됩니다.

<br/>


5. **알림 요청에 대한 사용자 응답**

    - 클라이언트는 알림 요청이 성공적으로 접수되었음을 확인하는 응답을 받습니다.

    - 응답에는 요청 ID와 상태가 포함됩니다.

<br/>
<br/>

---

### # 백그라운드 전송 프로세스

<br/>


1. **스케줄링을 통한 RequestOutbox 데이터 조회**

    - RequestOutbox 테이블의 NextRetryAt(ScheduledAt) 필드를 기준으로 스케줄러가 주기적으로 알림 요청을 조회합니다.
    조회된 RequestOutbox 데이터를 이벤트 발행을 통해 백그라운드 프로세스에 전달합니다.

    - 상태가 PENDING인 알림 요청을 찾습니다.

    - 조회된 알림 요청이 없으면 다음 스케줄링을 기다립니다.

<br/>

2. **NotificationRequest 도메인 객체 조회**

    - 스케줄러가 조회한 RequestOutbox의 AggreatedId를 사용하여 DB Adapter를 통해 NotificationRequest 도메인 객체를 조회합니다.

    - NotificationRequest 도메인 객체를 가져와 상태를 확인합니다.

    - 상태가 CANCELED라면 Outbox 이벤트를 삭제하고 처리를 중단합니다.

    - 상태가 PENDING이 아니면 처리하지 않습니다.

    - 상태가 PENDING이면 markAsProcessing() 메서드를 호출하여 상태를 PROCESSING으로 변경합니다.

<br/>

3. **NotificationRequest 파싱 & NotificationMessage 생성**

    - NotificationRequest 도메인 객체에서 알림 요청 정보를 파싱합니다.

    - 알림 요청의 수신자 목록, 발신자 정보, 알림 채널 등을 기반으로 NotificationMessage 객체를 생성합니다.

    - 이때, TemplateService를 사용하여 템플릿을 적용하고, User정보를 조회하여 수신자별로 알림 메시지를 생성합니다.

    - 파싱과 생성에 실패(Retry-Max 포함)하면 markAsFailed(reason) 메서드를 호출하여 상태를 FAILED로 변경하고 RequestOutbox를 삭제합니다.

<br/>

4. **NotificationMessage & MessageOutbox 저장**

    - NotificationMessage 객체를 MessageOutbox로 변환합니다.

    - MessageOutbox는 알림 요청의 상태, Payload(알림 메시지 데이터), NextRetryAt(전송 예정 시간) 등을 포함합니다.

    - DB Adapter를 통해 NotificationMessage와 MessageOutbox를 하나의 트랜잭션으로 저장합니다.

    - 이때, NotificationRequest의 processedAt 필드를 현재 시각으로 설정합니다.

    - 저장이 완료되면 NotificationMessageReadyEvent이벤트 발생과, RequestOutbox 데이터를 삭제합니다.

<br/>

5. **스케줄링을 통한 MessageOutbox 조회**

    - 스케줄러가 주기적으로 MessageOutbox 테이블을 조회하여 전송 예정 시간(NextRetryAt)이 지난 알림 메시지를 찾습니다.
    조회된 MessageOutbox 데이터를 이벤트 발행을 통해 백그라운드 프로세스에 전달합니다.

    - MessageOutbox의 AggreatedId를 사용하여 NotificationMessage 도메인 객체를 조회합니다.

    - 상태가 PENDING인 알림 메시지를 조회합니다.

    - 조회된 알림 메시지가 없으면 다음 스케줄링을 기다립니다.

<br/>

6. **NotificationMessage 메시지 전송**

    - 조회된 NotificationMessage를 MQ Adapter를 통해 Kafka로 전송합니다.

    - 이때, 알림 채널에 따라 적절한 전송 로직을 선택합니다 (예: EMAIL, SMS 등).

    - 전송이 완료되면 NotificationMessage 객체의 markAsDispatched() 메서드를 호출하여 상태를 DISPATCHED로 변경합니다.

    - 전송 실패(Retry-Max 포함)시 markAsFailed(reason) 메서드를 호출하여 상태를 FAILED로 변경하고 MessageOutbox를 삭제합니다.

<br/>
<br/>


## # 기술적 이슈와 해결방안

<br/>

- 왜 알림 요청을 비동기적으로 처리하는가?

    - 알림 요청은 사용자에게 즉시 응답을 제공해야 하며, 실제 알림 전송은 백그라운드에서 처리되어야 합니다. 
    이를 통해 사용자 경험을 향상시키고, 시스템의 부하를 분산시킬 수 있습니다.

    - 알림 요청을 비동기적으로 처리하기 위해 Outbox 패턴을 사용합니다. 
    Outbox 패턴은 데이터베이스에 저장된 이벤트를 기반으로 백그라운드 프로세스가 작업을 수행하도록 합니다. 
    이를 통해 데이터 일관성을 유지하면서도 비동기 처리를 구현할 수 있습니다.

<br/>

- NotificationRequest와 NotificationMessage를 분리하는 이유는 무엇인가?

    - NotificationRequest는 사용자 요청을 나타내며, 알림 메시지의 생성 및 전송과 관련된 정보를 포함합니다. 
    반면, NotificationMessage는 실제 전송될 알림 메시지를 나타냅니다.

    - 이 둘을 분리함으로써, 알림 요청의 상태 관리와 알림 메시지의 전송 로직을 독립적으로 처리할 수 있습니다. 
    이를 통해 시스템의 유연성과 확장성을 높일 수 있습니다.

    - 또한, 각각의 Outbox 테이블을 사용하여 상태를 관리함으로써, 각 단계별로 트랜잭션을 분리하고, 실패 시 롤백, 리트라이를 쉽게 구현할 수 있습니다.

<br/>

- 즉시 / 지연 전송을 어떻게 처리하는가?

    - 즉시 전송은 사용자의 요청인 NotificationRequest의 ScheduledAt 필드를 기준으로 처리합니다.
    ScheduledAt 필드를 Null로 설정하면, 요청로직에서 즉시 이벤트를 발생시켜 백그라운드 메세지 전송 프로세스가 시작됩니다.
    ScheduledAt 필드가 특정 시간으로 설정된 경우, 스케줄러가 해당 시각에 도달한 알림 메시지를 조회하고 전송합니다.

    - 사용자 요청 로직과 분리함과 동시에 전송 프로세스를 비동기적으로 처리하기 위해 SpringEvent를 사용합니다. 
    이를 통해 알림 요청이 접수되면 즉시 NotificationRequestReceivedEvent 이벤트가 발생하고, 백그라운드 프로세스가 이를 처리합니다.

<br/>

- 재시도 정책은 어떻게 구현하는가?

    - Outbox 상태가 PENDING인 경우, 스케줄러가 주기적으로 조회하여 전송 예정 시간(NextRetryAt)이 지난 메시지를 찾습니다.

    - 전송 실패 시, markAsFailed(reason) 메서드를 호출하여 상태를 FAILED로 변경하고, 실패 사유를 기록합니다. 
    이때, Retry-Max 횟수를 초과하면 더 이상 재시도하지 않습니다.

    - 재시도 로직은 Outbox 테이블에서 관리되며, 각 메시지의 NextRetryAt 필드를 업데이트하여 다음 재시도를 위한 시간을 설정합니다.

    - 외부 시스템과의 통신 실패나 일시적인 오류로 인해 전송이 실패할 수 있으므로, 재시도 로직을 통해 안정성을 높입니다. 

    - 4xx 에러가 발생하면, 해당 메시지는 잘못된 요청으로 판단되어 도메인 객체를 DEAD 상태로 변경하고 Outbox를 삭제합니다.

    - 5xx 에러가 발생하면, 해당 메시지는 일시적인 오류로 판단되어 재시도 로직을 통해 다시 전송을 시도합니다.

<br/>

- 알림 요청의 상태 관리는 어떻게 이루어지는가?

    - 알림 요청의 상태는 NotificationRequest객체와 NotificationMessage에서 관리됩니다.

    - 초기 상태는 PENDING이며, 전송 프로세스에 따라 PROCESSING, DISPATCHED, FAILED 등의 상태로 변경됩니다.

    - 각 상태 변경은 도메인 객체의 메서드를 통해 이루어지며, 상태 변경 시 이벤트가 발생하여 관련된 로직이 실행됩니다.

    - 예를 들어, markAsProcessing() 메서드는 상태를 PROCESSING으로 변경하고, markAsDispatched() 메서드는 상태를 DISPATCHED로 변경합니다.

    - 상태 변경은 데이터베이스 트랜잭션을 통해 일관성을 유지하며, 실패 시 롤백이 가능합니다.

<br/>

- 알림 요청의 이벤트 처리는 어떻게 이루어지는가?

    - 알림 요청의 이벤트 처리는 메인 로직과 별도로 처리하기 위해 Spring Event를 사용하여 구현됩니다.

    - NotificationRequestReceivedEvent, NotificationMessageReadyEvent 등의 이벤트가 발생하며, 이를 통해 백그라운드 프로세스가 트리거됩니다.

    - 이벤트 리스너는 해당 이벤트를 수신하고, 필요한 로직을 실행합니다. 
    예를 들어, NotificationRequestReceivedEvent가 발생하면 백그라운드 전송 프로세스를 시작합니다.

    - 이벤트 기반 아키텍처를 통해 시스템의 확장성과 유연성을 높일 수 있습니다. 
    이후 외부 MQ 시스템으로 알림 메시지를 전송할 때도 이벤트를 통해 처리할 수 있습니다.
