# notification-server

    /**
     * 1. 알림요청과 아웃박스1 저장
     * 2. 알림요청 이벤트 발행 (after commit)
     * - 알림요청이 즉시 처리되는 경우에는 NotificationRequestReceivedEvent를 발행합니다.
     * - 알림요청이 스케줄링된 경우에는 이벤트를 발행하지 않습니다. -> 이벤트 발행은 outbox1 스케줄링으로 처리합니다.
     * 
     * 
     * 3. NotificationRequestReceivedEvent 이벤트를 리슨
     * - 알림요청 Processing 상태 변경
     * - 취소 요청 확인해서 취소처리
     * 4. 알림 요청에 대한 알림 메세지 리스트 생성
     * 5. 개별 메세지와 아웃박스2 저장
     * 6. 개별 메세지 이벤트 발행 (after commit)
     * - 알림요청이 즉시 처리되는 경우에는 NotificationMessageReadyEvent 발행합니다.
     * - 알림요청이 스케줄링된 경우에는 이벤트를 발행하지 않습니다. -> 이벤트 발행은 outbox2 스케줄링으로 처리합니다.
     * 7. NotificationRequestReceivedEvent의 아웃박스 메세지 삭제
     * 
     * 
     * 7. NotificationMessageReadyEvent 이벤트를 리슨해서 MQ에 메세지 발행
     * 8. 알림요청의 상태를 Dispatched로 변경
     * 8. 큐에서 메세지 수신해서 실제 발송
     * - 알림 발송 성공. 알림요청 Dispachted 상태 변경
     * 9. NotificationMessageReadyEvent의 아웃박스 메세지 삭제
     * 
     * 
     * -- 아웃박스 리트라이 스케줄러 동작 (outbox 스케줄링으로 처리)
     * - status가 PENDING & FAILED인 OutboxMessage를 조회
     * - && nextRetryAt이 현재 시간보다 이전인 OutboxMessage를 조회
     * -- 2. outboxMessage를 조회해서 이벤트 객체로 파싱한 뒤 이벤트 발행
     */
