package notification.adapter.db;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

import lombok.Builder;
import lombok.Getter;
import notification.application.idempotency.Idempotency;
import notification.definition.utils.InstantDateTimeBridge;
import notification.definition.vo.JsonPayload;

@Getter
@Table("idempotency_key")
public class IdempotencyEntity implements Persistable<String> {

    @Id
    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("operation_type")
    private String operationType;

    @Column("data")
    private String data;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Builder
    public IdempotencyEntity(String idempotencyKey, String operationType, String data, LocalDateTime createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.operationType = operationType;
        this.data = data;
        this.createdAt = createdAt;
    }

    /**
     * IdempotencyEntity to Domain Object
     * 
     * @return Idempotency
     */
    public Idempotency toDomain() {
        return new Idempotency(
                idempotencyKey,
                operationType,
                new JsonPayload(data),
                InstantDateTimeBridge.toInstant(createdAt));
    }

    /**
     * Convert Domain Object to IdempotencyEntity
     * 
     * @param idempotency the domain object to convert
     * @return IdempotencyEntity
     */
    public static IdempotencyEntity fromDomain(Idempotency idempotency) {
        return IdempotencyEntity.builder()
                .idempotencyKey(idempotency.idempotencyKey())
                .operationType(idempotency.operationType())
                .data(idempotency.data().value())
                .createdAt(InstantDateTimeBridge.toLocalDateTime(idempotency.createdAt()))
                .build();
    }

    @Override
    @Nullable
    public String getId() {
        return idempotencyKey;
    }

    @Override
    public boolean isNew() {
        return true;
    }

}
