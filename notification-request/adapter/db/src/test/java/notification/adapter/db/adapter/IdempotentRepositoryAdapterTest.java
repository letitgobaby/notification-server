package notification.adapter.db.adapter;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import notification.adapter.db.MariadbTestContainerConfig;
import notification.adapter.db.repository.R2dbcIdempotencyRepository;
import notification.application.idempotency.Idempotency;
import notification.definition.exceptions.DuplicateRequestException;
import notification.definition.vo.JsonPayload;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import({ IdempotentRepositoryAdapter.class, ObjectMapper.class })
class IdempotentRepositoryAdapterTest extends MariadbTestContainerConfig {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private R2dbcIdempotencyRepository r2dbcIdempotencyRepository;

    private IdempotentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdempotentRepositoryAdapter(databaseClient, r2dbcIdempotencyRepository);
        // Clear the database before each test
        r2dbcIdempotencyRepository.deleteAll().block();
    }

    @Test
    @DisplayName("새로운 아이덴포턴시 키로 저장하면 성공해야 한다")
    void save_shouldSuccessfullyForNewIdempotencyKey() {
        Idempotency idempotency = new Idempotency(
                "test-key-1",
                "CREATE_NOTIFICATION",
                new JsonPayload("{\"title\":\"Test\"}"),
                Instant.now());

        //
        StepVerifier.create(adapter.save(idempotency))
                .assertNext(savedIdempotency -> {
                    assertThat(savedIdempotency.idempotencyKey()).isEqualTo("test-key-1");
                    assertThat(savedIdempotency.operationType()).isEqualTo("CREATE_NOTIFICATION");
                    assertThat(savedIdempotency.data().value()).isEqualTo("{\"title\":\"Test\"}");
                    assertThat(savedIdempotency.createdAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("중복된 아이덴포턴시 키로 저장하면 DuplicateRequestException이 발생해야 한다")
    void save_shouldThrowDuplicateRequestExceptionForDuplicateKey() {
        Idempotency idempotency1 = new Idempotency(
                "duplicate-key",
                "CREATE_NOTIFICATION",
                new JsonPayload("{\"title\":\"Test1\"}"),
                Instant.now());

        Idempotency idempotency2 = new Idempotency(
                "duplicate-key",
                "CREATE_NOTIFICATION",
                new JsonPayload("{\"title\":\"Test2\"}"),
                Instant.now());

        //
        StepVerifier.create(adapter.save(idempotency1))
                .expectNextCount(1)
                .verifyComplete();

        //
        StepVerifier.create(adapter.save(idempotency2))
                .expectError(DuplicateRequestException.class)
                .verify();
    }

    @Test
    @DisplayName("저장된 아이덴포턴시 키와 작업 유형으로 조회하면 해당 데이터를 반환해야 한다")
    void findById_shouldReturnDataForExistingIdempotencyKeyAndOperationType() {
        Idempotency originalIdempotency = new Idempotency(
                "find-test-key",
                "CREATE_NOTIFICATION",
                new JsonPayload("{\"title\":\"Find Test\"}"),
                Instant.now());

        //
        StepVerifier.create(adapter.save(originalIdempotency))
                .expectNextCount(1)
                .verifyComplete();

        //
        StepVerifier.create(adapter.findById("find-test-key", "CREATE_NOTIFICATION"))
                .assertNext(foundIdempotency -> {
                    assertThat(foundIdempotency.idempotencyKey()).isEqualTo("find-test-key");
                    assertThat(foundIdempotency.operationType()).isEqualTo("CREATE_NOTIFICATION");
                    assertThat(foundIdempotency.data().value()).isEqualTo("{\"title\":\"Find Test\"}");
                    assertThat(foundIdempotency.createdAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 아이덴포턴시 키로 조회하면 빈 결과를 반환해야 한다")
    void findById_shouldReturnEmptyForNonExistingIdempotencyKey() {
        // When & Then
        StepVerifier.create(adapter.findById("non-existing-key", "CREATE_NOTIFICATION"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하는 키지만 다른 작업 유형으로 조회하면 빈 결과를 반환해야 한다")
    void findById_shouldReturnEmptyForExistingKeyButDifferentOperationType() {
        // Given
        Idempotency idempotency = new Idempotency(
                "existing-key",
                "CREATE_NOTIFICATION",
                new JsonPayload("{\"title\":\"Test\"}"),
                Instant.now());

        // Save first
        StepVerifier.create(adapter.save(idempotency))
                .expectNextCount(1)
                .verifyComplete();

        // When & Then - Search with different operation type
        StepVerifier.create(adapter.findById("existing-key", "UPDATE_NOTIFICATION"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("JSON 페이로드가 복잡한 객체여도 저장하고 조회할 수 있어야 한다")
    void save_shouldHandleComplexJsonPayload() {
        String complexJson = "{\"notification\":{\"title\":\"Complex Test\",\"body\":\"Test Body\",\"recipients\":[\"user1\",\"user2\"],\"metadata\":{\"priority\":\"high\",\"category\":\"system\"}}}";
        Idempotency idempotency = new Idempotency(
                "complex-json-key",
                "CREATE_NOTIFICATION",
                new JsonPayload(complexJson),
                Instant.now());

        //
        StepVerifier.create(adapter.save(idempotency))
                .expectNextCount(1)
                .verifyComplete();

        //
        StepVerifier.create(adapter.findById("complex-json-key", "CREATE_NOTIFICATION"))
                .assertNext(foundIdempotency -> {
                    assertThat(foundIdempotency.data().value()).isEqualTo(complexJson);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("데이터베이스 오류가 DuplicateKeyException이 아닌 경우 원래 예외를 전파해야 한다")
    void save_shouldPropagateNonDuplicateKeyExceptions() {
        Idempotency invalidIdempotency = new Idempotency(
                null, // This should cause a constraint violation
                "CREATE_NOTIFICATION",
                new JsonPayload("{\"title\":\"Test\"}"),
                Instant.now());

        //
        StepVerifier.create(adapter.save(invalidIdempotency))
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("빈 JSON 페이로드로도 저장하고 조회할 수 있어야 한다")
    void save_shouldHandleEmptyJsonPayload() {
        Idempotency idempotency = new Idempotency(
                "empty-json-key",
                "CREATE_NOTIFICATION",
                new JsonPayload("{}"),
                Instant.now());

        //
        StepVerifier.create(adapter.save(idempotency))
                .expectNextCount(1)
                .verifyComplete();

        //
        StepVerifier.create(adapter.findById("empty-json-key", "CREATE_NOTIFICATION"))
                .assertNext(foundIdempotency -> {
                    assertThat(foundIdempotency.data().value()).isEqualTo("{}");
                })
                .verifyComplete();
    }

}
