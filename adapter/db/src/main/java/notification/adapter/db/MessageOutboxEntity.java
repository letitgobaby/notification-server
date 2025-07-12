package notification.adapter.db;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

import lombok.Builder;
import lombok.Getter;
import notification.definition.enums.OutboxStatus;
import notification.definition.utils.InstantDateTimeBridge;
import notification.definition.vo.JsonPayload;
import notification.definition.vo.outbox.MessageOutbox;
import notification.definition.vo.outbox.OutboxId;

@Getter
@Table("message_outbox")
public class MessageOutboxEntity implements Persistable<String> {

    @Id
    @Column("outbox_id")
    private String outboxId;

    @Column("aggregate_id")
    private String aggregateId;

    @Column("payload")
    private String payload;

    @Column("status")
    private String status; // 메시지 상태 (PENDING, FAILED)

    @Column("processed_at")
    private LocalDateTime processedAt; // 처리된 시간 추가

    @Column("retry_attempts")
    private int retryAttempts; // 재시도 횟수

    @Column("next_retry_at")
    private LocalDateTime nextRetryAt; // 다음 재시도 예정 시각 == 알림 발송 시각

    @Column("created_at")
    private LocalDateTime createdAt;

    @Builder
    public MessageOutboxEntity(String outboxId, String aggregateId, String payload, String status,
            LocalDateTime processedAt, int retryAttempts, LocalDateTime nextRetryAt,
            LocalDateTime createdAt) {
        this.outboxId = outboxId;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = status;
        this.processedAt = processedAt;
        this.retryAttempts = retryAttempts;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = createdAt;
    }

    public MessageOutbox toDomain() {
        return new MessageOutbox(
                new OutboxId(this.getOutboxId()),
                this.getAggregateId(),
                new JsonPayload(this.getPayload()),
                this.getRetryAttempts(),
                InstantDateTimeBridge.toInstant(this.getNextRetryAt()),
                OutboxStatus.valueOf(this.getStatus()),
                InstantDateTimeBridge.toInstant(this.getProcessedAt()),
                InstantDateTimeBridge.toInstant(this.getCreatedAt()));
    }

    public static MessageOutboxEntity fromDomain(MessageOutbox domain) {
        return MessageOutboxEntity.builder()
                .outboxId(domain.getOutboxId().value())
                .aggregateId(domain.getAggregateId())
                .payload(domain.getPayload().value())
                .status(domain.getStatus().name())
                .processedAt(InstantDateTimeBridge.toLocalDateTime(domain.getProcessedAt()))
                .retryAttempts(domain.getRetryAttempts())
                .nextRetryAt(InstantDateTimeBridge.toLocalDateTime(domain.getNextRetryAt()))
                .createdAt(InstantDateTimeBridge.toLocalDateTime(domain.getCreatedAt()))
                .build();
    }

    @Override
    @Nullable
    public String getId() {
        return this.outboxId;
    }

    @Override
    public boolean isNew() {
        // 새 엔티티인지 여부를 판단하기 위해 createdAt이 null인지 확인
        boolean isNew = this.createdAt == null;
        if (isNew) {
            // 새 엔티티인 경우 createdAt을 현재 시간으로 설정
            this.createdAt = LocalDateTime.now();
        }
        return isNew;
    }
}
