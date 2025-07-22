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
import notification.adapter.db.repository.R2dbcMessageOutboxRepository;
import notification.definition.enums.OutboxStatus;
import notification.definition.vo.JsonPayload;
import notification.definition.vo.outbox.MessageOutbox;
import notification.definition.vo.outbox.OutboxId;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import({ MessageOutboxRepositoryAdapter.class })
public class MessageOutboxRepositoryAdapterTest extends MariadbTestContainerConfig {

    @Autowired
    private MessageOutboxRepositoryAdapter messageOutboxRepositoryAdapter;

    @Autowired
    private R2dbcMessageOutboxRepository r2dbcMessageOutboxRepository;

    // Test data fields
    private OutboxId outboxId1, outboxId2;
    private String aggregateId1, aggregateId2;
    private JsonPayload payload1, payload2;
    private MessageOutbox outbox1, outbox2;
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
        outbox1 = new MessageOutbox(
                outboxId1, aggregateId1, payload1,
                0, testTime.plusSeconds(300), OutboxStatus.PENDING, null, null);

        outbox2 = new MessageOutbox(
                outboxId2, aggregateId2, payload2,
                1, testTime.plusSeconds(600), OutboxStatus.FAILED, null, null);
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 모든 데이터 정리
        r2dbcMessageOutboxRepository.deleteAll().block();
    }

    @Test
    @DisplayName("MessageOutbox를 저장할 수 있다")
    void save_shouldSaveMessageOutbox() {
        // when & then
        StepVerifier.create(
                messageOutboxRepositoryAdapter.save(outbox1))
                .assertNext(savedOutbox -> {
                    assertThat(savedOutbox).isNotNull();
                    assertThat(savedOutbox.getOutboxId()).isEqualTo(outbox1.getOutboxId());
                    assertThat(savedOutbox.getAggregateId()).isEqualTo(outbox1.getAggregateId());
                    assertThat(savedOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("ID로 MessageOutbox를 찾을 수 있다")
    void findById_shouldReturnMessageOutbox_whenExists() {
        // given - MessageOutbox를 먼저 저장
        messageOutboxRepositoryAdapter.save(outbox1).block();

        // when & then
        StepVerifier.create(messageOutboxRepositoryAdapter.findById(outboxId1))
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

        StepVerifier.create(messageOutboxRepositoryAdapter.findById(nonExistentId))
                .verifyComplete();
    }

    @Test
    @DisplayName("AggregateId로 MessageOutbox 리스트를 찾을 수 있다")
    void findByAggregateId_shouldReturnMessageOutboxList() {
        // given - 같은 aggregateId로 두 개의 MessageOutbox 생성
        MessageOutbox outbox2SameAggregate = new MessageOutbox(
                OutboxId.generate(),
                aggregateId1, // same aggregate ID
                JsonPayload.of("{\"message\": \"test2\"}"),
                0,
                testTime.plusSeconds(600),
                OutboxStatus.PENDING,
                null,
                null // createdAt을 null로 설정
        );

        // MessageOutbox들을 저장
        messageOutboxRepositoryAdapter.save(outbox1).block();
        messageOutboxRepositoryAdapter.save(outbox2SameAggregate).block();

        // when & then
        StepVerifier.create(messageOutboxRepositoryAdapter.findByAggregateId(aggregateId1))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("AggregateId로 MessageOutbox들을 삭제할 수 있다")
    void deleteByAggregateId_shouldDeleteMessageOutboxes() {
        // given - MessageOutbox를 저장
        messageOutboxRepositoryAdapter.save(outbox1).block();

        // when - AggregateId로 삭제
        StepVerifier.create(messageOutboxRepositoryAdapter.deleteByAggregateId(aggregateId1))
                .verifyComplete();

        // then - 삭제되었는지 확인
        StepVerifier.create(messageOutboxRepositoryAdapter.findByAggregateId(aggregateId1))
                .verifyComplete();
    }

    @Test
    @DisplayName("ID로 MessageOutbox를 삭제할 수 있다")
    void deleteById_shouldDeleteMessageOutbox() {
        // given - MessageOutbox를 저장
        messageOutboxRepositoryAdapter.save(outbox1).block();

        // when - ID로 삭제
        StepVerifier.create(messageOutboxRepositoryAdapter.deleteById(outboxId1))
                .verifyComplete();

        // then - 삭제되었는지 확인
        StepVerifier.create(messageOutboxRepositoryAdapter.findById(outboxId1))
                .verifyComplete();
    }

    @Test
    @DisplayName("처리할 수 있는 Pending 상태의 Outbox를 조회할 수 있다")
    void fetchOutboxToProcess_shouldFetchPendingOutboxes() {
        // given - 처리 가능한 시간의 PENDING 메시지 생성
        MessageOutbox pendingOutbox = new MessageOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                0,
                testTime.plusSeconds(60), // 미래 시간이지만 fetchTime보다는 과거
                OutboxStatus.PENDING,
                null,
                null // createdAt을 null로 설정
        );

        messageOutboxRepositoryAdapter.save(pendingOutbox).block();

        // when & then - fetchTime을 미래로 설정하여 위 메시지가 조회되도록 함
        StepVerifier.create(messageOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 10))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("처리할 수 있는 Failed 상태의 Outbox를 조회할 수 있다")
    void fetchOutboxToProcess_shouldFetchFailedOutboxes() {
        // given - 처리 가능한 시간의 FAILED 메시지 생성
        MessageOutbox failedOutbox = new MessageOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                2,
                testTime.plusSeconds(60), // 미래 시간이지만 fetchTime보다는 과거
                OutboxStatus.FAILED,
                null,
                null // createdAt을 null로 설정
        );

        messageOutboxRepositoryAdapter.save(failedOutbox).block();

        // when & then
        StepVerifier.create(messageOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 10))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("미래 시간의 Outbox는 조회되지 않는다")
    void fetchOutboxToProcess_shouldNotFetchFutureOutboxes() {
        // given - 미래 시간의 메시지 생성
        MessageOutbox futureOutbox = new MessageOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                0,
                testTime.plusSeconds(3600), // 1시간 후
                OutboxStatus.PENDING,
                null,
                null // createdAt을 null로 설정
        );

        messageOutboxRepositoryAdapter.save(futureOutbox).block();

        // when & then - 현재 시간으로 조회하면 미래 메시지는 조회되지 않아야 함
        StepVerifier.create(messageOutboxRepositoryAdapter.fetchOutboxToProcess(testTime, 10))
                .verifyComplete();
    }

    @Test
    @DisplayName("SENT 상태의 Outbox는 조회되지 않는다")
    void fetchOutboxToProcess_shouldNotFetchSentOutboxes() {
        // given - SENT 상태의 메시지 생성
        MessageOutbox sentOutbox = new MessageOutbox(
                OutboxId.generate(),
                aggregateId1,
                payload1,
                0,
                testTime.plusSeconds(60), // 과거 시간
                OutboxStatus.SENT,
                testTime.plusSeconds(30),
                null // createdAt을 null로 설정
        );

        messageOutboxRepositoryAdapter.save(sentOutbox).block();

        // when & then
        StepVerifier.create(messageOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 10))
                .verifyComplete();
    }

    @Test
    @DisplayName("Limit 파라미터를 준수한다")
    void fetchOutboxToProcess_shouldRespectLimit() {
        // given - 여러 개의 처리 가능한 메시지 생성
        Flux.range(1, 5)
                .map(i -> new MessageOutbox(
                        OutboxId.generate(),
                        "aggregate-" + i,
                        JsonPayload.of("{\"message\": \"test" + i + "\"}"),
                        0,
                        testTime.plusSeconds(60), // 미래 시간이지만 fetchTime보다는 과거
                        OutboxStatus.PENDING,
                        null,
                        null // createdAt을 null로 설정
                ))
                .flatMap(messageOutboxRepositoryAdapter::save)
                .blockLast();

        // when & then - limit을 3으로 설정
        StepVerifier.create(messageOutboxRepositoryAdapter.fetchOutboxToProcess(testTime.plusSeconds(120), 3))
                .expectNextCount(3)
                .verifyComplete();
    }
}