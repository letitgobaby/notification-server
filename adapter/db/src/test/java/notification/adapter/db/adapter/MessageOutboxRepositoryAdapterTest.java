package notification.adapter.db.adapter;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import notification.adapter.db.MariadbTestContainerConfig;
import notification.adapter.db.repository.R2dbcMessageOutboxRepository;
import notification.definition.enums.OutboxStatus;
import notification.definition.vo.JsonPayload;
import notification.definition.vo.outbox.MessageOutbox;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import(MessageOutboxRepositoryAdapter.class)
public class MessageOutboxRepositoryAdapterTest extends MariadbTestContainerConfig {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private R2dbcMessageOutboxRepository r2dbcMessageOutboxRepository;

    private MessageOutboxRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MessageOutboxRepositoryAdapter(r2dbcMessageOutboxRepository, databaseClient);
    }

    @Test
    void save_and_findById_should_work() {
        MessageOutbox message = MessageOutbox.create("aggreagteId",
                new JsonPayload("{\"message\": \"hi\"}"), Instant.now());

        //
        MessageOutbox saved = adapter.save(message).block();
        MessageOutbox found = adapter.findById(message.getOutboxId()).block();

        //
        assertNotNull(saved);
        assertNotNull(found);
        assertEquals(saved.getOutboxId(), found.getOutboxId());
        assertEquals(saved.getAggregateId(), found.getAggregateId());
    }

    @Test
    void deleteByAggregateId_should_work() {
        String aggregateId = "agg-123";
        MessageOutbox message = MessageOutbox.create(aggregateId,
                new JsonPayload("{\"message\": \"hi\"}"), Instant.now());

        //
        adapter.save(message).block();
        adapter.deleteByAggregateId(aggregateId).block();

        //
        StepVerifier.create(adapter.findById(message.getOutboxId()))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void findPendingAndFailedMessages_should_return_result() {
        int saveCount = 3;
        String aggregateId = "agg-123";
        String payload = "{\"event\": \"retry\"}";

        //
        for (int i = 0; i < saveCount; i++) {
            MessageOutbox message = MessageOutbox.create(aggregateId,
                    new JsonPayload(payload), Instant.now());

            adapter.save(message).block();
        }

        //
        StepVerifier.create(adapter.findPendingAndFailedMessages())
                .expectNextCount(saveCount)
                .assertNext(message -> {
                    assertEquals(aggregateId, message.getAggregateId());
                    assertEquals(payload, message.getPayload().value());
                    assertEquals(OutboxStatus.PENDING, message.getStatus());
                });
    }
}