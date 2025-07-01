package notification.adapter.db.adapter;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import notification.adapter.db.MariadbTestContainerConfig;
import notification.adapter.db.repository.R2dbcRequestOutboxRepository;
import notification.definition.vo.JsonPayload;
import notification.definition.vo.outbox.RequestOutbox;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import(RequestOutboxRepositoryAdapter.class)
class RequestOutboxRepositoryAdapterTest extends MariadbTestContainerConfig {

    @Autowired
    private R2dbcRequestOutboxRepository r2dbcRequestOutboxRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private RequestOutboxRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RequestOutboxRepositoryAdapter(r2dbcRequestOutboxRepository, databaseClient);
    }

    private RequestOutbox getDomain() {
        return RequestOutbox.create(
                "aggreagteId",
                new JsonPayload("{\"message\": \"hi\"}"),
                Instant.now());
    }

    @Test
    void save_shouldReturnSavedDomain() {
        RequestOutbox domain = getDomain();

        StepVerifier.create(adapter.save(domain))
                .assertNext(saved -> saved.getAggregateId().equals(domain.getAggregateId()));
    }

    @Test
    void findById_shouldReturnDomain() {
        RequestOutbox domain = getDomain();

        adapter.save(domain).block();

        StepVerifier.create(adapter.findById(domain.getOutboxId()))
                .expectNextMatches(found -> found.getAggregateId().equals(domain.getAggregateId()));
    }

    @Test
    void deleteByAggregateId_shouldCallRepository() {
        RequestOutbox domain = getDomain();

        adapter.save(domain).block();

        StepVerifier.create(adapter.deleteByAggregateId(domain.getAggregateId()))
                .verifyComplete();
    }

    @Test
    void deleteById_shouldCallRepository() {
        RequestOutbox domain = getDomain();

        adapter.save(domain).block();

        StepVerifier.create(adapter.deleteById(domain.getOutboxId()))
                .verifyComplete();
    }

    @Test
    void findPendingAndFailedMessages_shouldReturnFluxOfDomain() {
        int saveCount = 3;
        String aggregateId = "agg-123";
        String payload = "{\"event\": \"retry\"}";

        for (int i = 0; i < saveCount; i++) {
            RequestOutbox domain = RequestOutbox.create(aggregateId,
                    new JsonPayload(payload), Instant.now());

            adapter.save(domain).block();
        }

        StepVerifier.create(adapter.findPendingAndFailedMessages())
                .expectNextCount(saveCount);
    }
}