graph TD
    subgraph Request Intake
        A[API Gateway/Client Request] --> B(NotificationRequestCreationService);
        B --> C{DB Transaction: Save NotificationRequest + OutboxMessage};
    end

    subgraph Outbox Relayer
        C --> D[Outbox Relayer];
        D --> E[MQ: notification.request.received];
    end

    subgraph Item Preparation
        E --> F[NotificationItemPreparationWorker];
        F --> G{DB Transaction: Save NotificationItem + OutboxMessage};
        G --> H[Outbox Relayer];
        H --> I[MQ: notification.item.scheduled (Delayed)];
    end

    subgraph Scheduling & Dispatch
        I --> J[NotificationItemScheduler (Delayed MQ Consumer)];
        J --> K[MQ: notification.item.dispatch.ready];
        K --> L[NotificationDispatcher (Actual Sender)];
        L --> M[External SMS/Email/Push Gateways];
        L --> N[MQ: notification.item.dispatched];
    end

    subgraph User Notification & Status Update
        N --> O[UserNotificationUpdater];
        O --> P[DB: UserNotification];

        M --> Q[Delivery Callback/Polling Service];
        Q --> R[MQ: notification.item.delivery.status.updated];

        R --> S[NotificationItemService (Update NotificationItem)];
        S --> T[DB: NotificationItem];

        R --> U[UserNotificationUpdater (Update UserNotification)];
        U --> P;

        F --Update Status--> V[DB: NotificationRequest];
        L --Update Status--> V;
        S --Update Status--> V;
        V --> W[MQ: notification.request.status.updated];
    end

    subgraph User Query
        P --> X[UserNotificationQueryService];
        X --> Y[User App/Web UI];
    end


    각 MQ 토픽 상세 설명

1. notification.request.received
발행자 (Producer): NotificationRequestCreationService (Outbox Relayer를 통해)
새로운 NotificationRequest가 성공적으로 저장될 때, Outbox 테이블에 NotificationRequestReceivedEvent를 기록합니다. Outbox Relayer가 이를 읽어 MQ에 발행합니다.
소비자 (Consumer): NotificationItemPreparationWorker
이 이벤트를 받아 NotificationRequest의 세부 정보를 파싱합니다.
TargetAudience에 따라 회원/비회원 연락처 정보를 조회하고, NotificationChannelConfig를 바탕으로 개별 NotificationItem들을 생성합니다.
NotificationItem들을 DB에 저장하고, 각 NotificationItem에 대해 notification.item.scheduled 토픽으로 지연 메시지를 발행합니다.
목적: 알림 서비스 내에서 새로운 알림 요청이 들어왔음을 알리고, 개별 NotificationItem 생성 및 스케줄링 프로세스를 시작합니다.
2. notification.item.scheduled (지연 메시지 큐 사용)
발행자 (Producer): NotificationItemPreparationWorker (Outbox Relayer를 통해)
NotificationItem이 생성될 때, NotificationItemScheduledEvent (혹은 단순히 NotificationItemId를 포함하는 메시지)를 해당 NotificationItem의 scheduledAt 시간에 맞춰 지연 메시지 큐에 발행합니다.
주의: 이 토픽은 MQ의 지연 메시지 기능을 사용하며, 메시지는 설정된 지연 시간 후에야 NotificationItemScheduler에게 전달됩니다.
소비자 (Consumer): NotificationItemScheduler (지연 MQ 컨슈머)
지연 시간이 만료되어 MQ로부터 메시지를 수신합니다.
메시지로부터 NotificationItemId를 추출하여 해당 NotificationItem을 DB에서 로드합니다.
로드된 NotificationItem을 notification.item.dispatch.ready 토픽으로 전달하여 실제 발송을 준비합니다. (이때 NotificationItem 전체 또는 발송에 필요한 핵심 정보만 전달).
목적: 특정 시간에 알림을 발송해야 하는 NotificationItem들을 정확한 시간에 발송 시스템으로 전달하기 위한 스케줄링.
3. notification.item.dispatch.ready
발행자 (Producer): NotificationItemScheduler
scheduledAt 시간이 되어 발송 준비가 완료된 NotificationItem에 대해 NotificationItemReadyForDispatchEvent를 발행합니다.
소비자 (Consumer): NotificationDispatcher (알림 발송 서버)
이 이벤트를 수신하여 실제 알림 발송을 수행합니다 (예: SMS 게이트웨이 호출, 이메일 전송 API 호출, 푸시 서버 API 호출).
이 단계는 알림 서비스의 외부 발송 연동 모듈 또는 전용 발송 서버에서 처리됩니다.
발송 시도 후, notification.item.dispatched 이벤트를 발행합니다.
목적: 실제 알림 채널(SMS, Email, Push)을 통해 알림을 발송하기 위한 명령.
4. notification.item.dispatched
발행자 (Producer): NotificationDispatcher (알림 발송 서버)
NotificationItem에 대한 실제 발송 시도가 완료된 직후 (성공이든 실패든 관계없이) NotificationItemDispatchedEvent를 발행합니다.
소비자 (Consumer): UserNotificationUpdater
이 이벤트를 수신하여 UserNotification 레코드를 생성하고 DB에 저장합니다. 이 시점부터 사용자는 자신의 알림 내역에서 해당 알림을 볼 수 있습니다.
목적: 알림이 실제 발송되기 시작했음을 알리고, 사용자에게 보여줄 알림 내역 (UserNotification)을 생성합니다.
5. notification.item.delivery.status.updated
발행자 (Producer): DeliveryCallbackService / DeliveryStatusPollingService (알림 발송 서버)
SMS, 이메일, 푸시 등의 외부 발송 시스템으로부터 실제 전달 성공/실패 콜백을 받거나, 주기적으로 발송 상태를 폴링하여 NotificationItem의 최종 전달 상태가 변경될 때 NotificationItemDeliveryStatusUpdatedEvent를 발행합니다.
소비자 (Consumer 1): NotificationItemService (알림 서비스 내의 NotificationItem 관리 서비스)
이 이벤트를 수신하여 원본 NotificationItem의 deliveryStatus 및 deliveryFailureReason을 업데이트합니다.
소비자 (Consumer 2): UserNotificationUpdater
이 이벤트를 수신하여 해당 UserNotification의 deliveryStatus를 업데이트합니다. (사용자 UI에 실제 발송 결과를 반영)
목적: 개별 알림의 최종적인 발송/수신 결과를 추적하고, 관련 모델(NotificationItem, UserNotification)의 상태를 업데이트합니다.
6. notification.request.status.updated
발행자 (Producer): NotificationRequestService (혹은 BroadcastNotificationProcessor와 같은 관련 워커)
NotificationRequest의 전체 상태(PENDING -> PROCESSING -> COMPLETED / FAILED)가 변경될 때 NotificationRequestStatusUpdatedEvent를 발행합니다.
소비자 (Consumer): 다른 도메인 서비스 (예: 주문 서비스, 사용자 서비스, 통계 서비스)
다른 서비스들이 알림 요청의 최종 결과를 알 필요가 있을 때 이 이벤트를 구독합니다.
목적: 알림 요청의 전체적인 생명주기 및 결과에 대한 정보를 다른 시스템과 공유합니다.
