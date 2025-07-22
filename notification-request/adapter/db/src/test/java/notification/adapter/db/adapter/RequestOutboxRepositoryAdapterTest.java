package notification.adapter.db.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import notification.adapter.db.MariadbTestContainerConfig;
import notification.adapter.db.repository.R2dbcRequestOutboxRepository;
import notification.definition.enums.OutboxStatus;
import notification.definition.vo.JsonPayload;
import notification.definition.vo.outbox.OutboxId;
import notification.definition.vo.outbox.RequestOutbox;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import({ RequestOutboxRepositoryAdapter.class })
public class RequestOutboxRepositoryAdapterTest extends MariadbTestContainerConfig {

    @Autowired
    private RequestOutboxRepositoryAdapter requestOutboxRepositoryAdapter;

    @Autowired
    private R2dbcRequestOutboxRepository r2dbcRequestOutboxRepository;

    // Test data fields
    private OutboxId outboxId1, outboxId2;
    private String aggregateId1, aggregateId2;
    private JsonPayload payload1, payload2;
    private RequestOutbox outbox1, outbox2;
    private Instant testTime;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testTime = Instant.now();
        outboxId1 = OutboxId.generate();
        outboxId2 = OutboxId.generate();
        aggregateId1 = "aggregate-1";
        aggregateId2 = "aggregate-2";

        payload1 = JsonPayload.of("{\"message\": \"test1\"}");
        payload2 = JsonPayload.of("{\"message\": \"test2\"}");

        // nextRetryAt을 충분히 미래로 설정, createdAt을 null로 설정하여 새 엔티티로 인식되도록
        outbox1 = new RequestOutbox(
                outboxId1, aggregateId1, payload1,
                0, testTime.plusSeconds(300), OutboxStatus.PENDING, null, null);

        outbox2 = new RequestOutbox(
                outboxId2, aggregateId2, payload2,
                1, testTime.plusSeconds(600), OutboxStatus.FAILED, null, null);
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 모든 데이터 정리
        r2dbcRequestOutboxRepository.deleteAll().block();
    }

    @Test
    @DisplayName("RequestOutbox를 저장할 수 있다")
    void save_shouldSaveRequestOutbox() {
        // when & then
        StepVerifier.create(
                requestOutboxRepositoryAdapter.save(outbox1))
                .assertNext(savedOutbox -> {
                    assertThat(savedOutbox).isNotNull();
                    assertThat(savedOutbox.getOutboxId()).isEqualTo(outbox1.getOutboxId());
                    assertThat(savedOutbox.getAggregateId()).isEqualTo(outbox1.getAggregateId());
                    assertThat(savedOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("ID로 RequestOutbox를 찾을 수 있다")
    void findById_shouldReturnRequestOutbox_whenExists() {
        // given - RequestOutbox를 먼저 저장
        requestOutboxRepositoryAdapter.save(outbox1).block();

        // when & then
        StepVerifier.create(requestOutboxRepositoryAdapter.findById(outboxId1))
                .assertNext(found -> {
                    assertEquals(outboxId1, found.getOutboxId());
                    assertEquals(aggregateId1, found.getAggregateId());
                    assertEquals(payload1.value(), found.getPayload().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 ID로 찾으면 empty를 반환한다")
    void findById_shouldReturnEmpty_whenNotExists() {
        OutboxId nonExistentId = OutboxId.generate();

        StepVerifier.create(requestOutboxRepositoryAdapter.findById(nonExistentId))
                .verifyComplete();
    }

    @Test
    @DisplayName("AggregateId로 RequestOutbox를 삭제할 수 있다")
    void deleteByAggregateId_shouldDeleteRequestOutboxes() {
        // given - RequestOutbox를 저장
        requestOutboxRepositoryAdapter.save(outbox1).block();

        // when - AggregateId로 삭제
        StepVerifier.create(requestOutboxRepositoryAdapter.deleteByAggregateId(aggregateId1))
                .verifyComplete();

        // then - 삭제되었는지 확인
        StepVerifier.create(requestOutboxRepositoryAdapter.findById(outboxId1))
                .verifyComplete();
    }

    @Test
    @DisplayName("ID로 RequestOutbox를 삭제할 수 있다")
    void deleteById_shouldDeleteRequestOutbox() {
        // given - RequestOutbox를 저장
        requestOutboxRepositoryAdapter.save(outbox1).block();

        // when - ID로 삭제
        StepVerifier.create(requestOutboxRepositoryAdapter.deleteById(outboxId1))
                .verifyComplete();

        // then - 삭제되었는지 확인
        StepVerifier.create(requestOutboxRepositoryAdapter.findById(outboxId1))
                .verifyComplete();
    }

    @Test
    @DisplayName("처리할 수 있는 Pending 상태의 Outbox를 조회할 수 있다")
    void fetchOutboxToProcess_shouldFetchPendingOutboxes() {
        // given - 처리 가능한 시간의 PENDING 메시지 생성
        RequestOutbox pendingOutbox = new RequestOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                0,
                testTime.plusSeconds(60), // 미래 시간이지만 fetchTime보다는 과거
                OutboxStatus.PENDING,
                null,
                null // createdAt을 null로 설정
        );

        requestOutboxRepositoryAdapter.save(pendingOutbox).block();

        // when & then - fetchTime을 미래로 설정하여 위 메시지가 조회되도록 함
        StepVerifier.create(requestOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 10))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("처리할 수 있는 Failed 상태의 Outbox를 조회할 수 있다")
    void fetchOutboxToProcess_shouldFetchFailedOutboxes() {
        // given - 처리 가능한 시간의 FAILED 메시지 생성
        RequestOutbox failedOutbox = new RequestOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                2,
                testTime.plusSeconds(60), // 미래 시간이지만 fetchTime보다는 과거
                OutboxStatus.FAILED,
                null,
                null // createdAt을 null로 설정
        );

        requestOutboxRepositoryAdapter.save(failedOutbox).block();

        // when & then
        StepVerifier.create(requestOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 10))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("미래 시간의 Outbox는 조회되지 않는다")
    void fetchOutboxToProcess_shouldNotFetchFutureOutboxes() {
        // given - 미래 시간의 메시지 생성
        RequestOutbox futureOutbox = new RequestOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                0,
                testTime.plusSeconds(3600), // 1시간 후
                OutboxStatus.PENDING,
                null,
                null // createdAt을 null로 설정
        );

        requestOutboxRepositoryAdapter.save(futureOutbox).block();

        // when & then - 현재 시간으로 조회하면 미래 메시지는 조회되지 않아야 함
        StepVerifier.create(requestOutboxRepositoryAdapter.fetchOutboxToProcess(testTime, 10))
                .verifyComplete();
    }

    @Test
    @DisplayName("SENT 상태의 Outbox는 조회되지 않는다")
    void fetchOutboxToProcess_shouldNotFetchSentOutboxes() {
        // given - SENT 상태의 메시지 생성
        RequestOutbox sentOutbox = new RequestOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                0,
                testTime.plusSeconds(60), // 과거 시간
                OutboxStatus.SENT,
                testTime.plusSeconds(30),
                null // createdAt을 null로 설정
        );

        requestOutboxRepositoryAdapter.save(sentOutbox).block();

        // when & then
        StepVerifier.create(requestOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 10))
                .verifyComplete();
    }

    @Test
    @DisplayName("IN_PROGRESS 상태의 Outbox는 조회되지 않는다")
    void fetchOutboxToProcess_shouldNotFetchInProgressOutboxes() {
        // given - IN_PROGRESS 상태의 메시지 생성
        RequestOutbox inProgressOutbox = new RequestOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                0,
                testTime.plusSeconds(60), // 과거 시간
                OutboxStatus.IN_PROGRESS,
                testTime.plusSeconds(30),
                null // createdAt을 null로 설정
        );

        requestOutboxRepositoryAdapter.save(inProgressOutbox).block();

        // when & then
        StepVerifier.create(requestOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 10))
                .verifyComplete();
    }

    @Test
    @DisplayName("Limit 파라미터를 준수한다")
    void fetchOutboxToProcess_shouldRespectLimit() {
        // given - 여러 개의 처리 가능한 메시지 생성
        Flux.range(1, 5)
                .map(i -> new RequestOutbox(
                        OutboxId.generate(),
                        "aggregate-" + i,
                        JsonPayload.of("{\"message\": \"test" + i + "\"}"),
                        0,
                        testTime.plusSeconds(60), // 미래 시간이지만 fetchTime보다는 과거
                        OutboxStatus.PENDING,
                        null,
                        null // createdAt을 null로 설정
                ))
                .flatMap(requestOutboxRepositoryAdapter::save)
                .blockLast();

        // when & then - limit을 3으로 설정
        StepVerifier.create(requestOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 3))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("fetchOutboxToProcess 호출 시 조회된 메시지들의 상태가 IN_PROGRESS로 변경된다")
    void fetchOutboxToProcess_shouldUpdateStatusToInProgress() {
        // given - 처리 가능한 시간의 PENDING 메시지 생성
        RequestOutbox pendingOutbox = new RequestOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                0,
                testTime.plusSeconds(60), // 미래 시간이지만 fetchTime보다는 과거
                OutboxStatus.PENDING,
                null,
                null // createdAt을 null로 설정
        );

        RequestOutbox savedOutbox = requestOutboxRepositoryAdapter.save(pendingOutbox).block();

        // when - fetchOutboxToProcess 호출
        StepVerifier.create(requestOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 10))
                .expectNextCount(1)
                .verifyComplete();

        // then - 저장된 outbox의 상태가 IN_PROGRESS로 변경되었는지 확인
        StepVerifier.create(requestOutboxRepositoryAdapter.findById(savedOutbox.getOutboxId()))
                .assertNext(found -> {
                    assertThat(found.getStatus()).isEqualTo(OutboxStatus.IN_PROGRESS);
                    assertThat(found.getProcessedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("처리 시간이 지난 메시지만 조회된다")
    void fetchOutboxToProcess_shouldOnlyFetchMessagesReadyForProcessing() {
        // given - 처리 시간이 지나지 않은 메시지와 지난 메시지 생성
        RequestOutbox notReadyOutbox = new RequestOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                0,
                testTime.plusSeconds(300), // 미래 시간 (처리할 수 없음)
                OutboxStatus.PENDING,
                null,
                null);

        RequestOutbox readyOutbox = new RequestOutbox(
                OutboxId.generate(),
                aggregateId2,
                payload2,
                0,
                testTime.plusSeconds(5), // 현재 시간보다 5초 후 (처리할 수 있음)
                OutboxStatus.PENDING,
                null,
                null);

        requestOutboxRepositoryAdapter.save(notReadyOutbox).block();
        requestOutboxRepositoryAdapter.save(readyOutbox).block();

        // when & then - 미래 시간으로 조회하면 처리 시간이 지난 메시지만 조회되어야 함
        StepVerifier.create(requestOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(60), 10))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("여러 상태의 RequestOutbox를 저장할 수 있다")
    void save_shouldSaveMultipleRequestOutboxes() {
        // when & then - outbox1 (PENDING) 저장
        StepVerifier.create(
                requestOutboxRepositoryAdapter.save(outbox1))
                .assertNext(savedOutbox -> {
                    assertThat(savedOutbox).isNotNull();
                    assertThat(savedOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
                })
                .verifyComplete();

        // when & then - outbox2 (FAILED) 저장
        StepVerifier.create(
                requestOutboxRepositoryAdapter.save(outbox2))
                .assertNext(savedOutbox -> {
                    assertThat(savedOutbox).isNotNull();
                    assertThat(savedOutbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
                    assertThat(savedOutbox.getRetryAttempts()).isEqualTo(1);
                })
                .verifyComplete();
    }
}