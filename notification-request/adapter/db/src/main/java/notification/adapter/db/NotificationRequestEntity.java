package notification.adapter.db;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

import lombok.Builder;
import lombok.Getter;
import notification.definition.utils.InstantDateTimeBridge;

@Getter
@Table("notification_request")
public class NotificationRequestEntity implements Persistable<String> {

    @Id
    @Column("request_id")
    private String requestId;

    @Column("requester_type")
    private String requesterType;

    @Column("requester_id")
    private String requesterId;

    @Column("notification_types")
    private String notificationTypes; // JSON serialized List<NotificationType>

    @Column("memo")
    private String memo;

    @Column("scheduled_at")
    private LocalDateTime scheduledAt;

    @Column("status")
    private String status; // RequestStatus enum을 String으로 저장

    @Column("failure_reason")
    private String failureReason;

    @Column("processed_at")
    private LocalDateTime processedAt;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Builder
    public NotificationRequestEntity(String requestId, String requesterType,
            String requesterId, String notificationTypes,
            String memo, LocalDateTime scheduledAt, String status,
            String failureReason, LocalDateTime processedAt, LocalDateTime createdAt) {
        this.requestId = requestId;
        this.requesterType = requesterType;
        this.requesterId = requesterId;
        this.notificationTypes = notificationTypes;
        this.memo = memo;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.failureReason = failureReason;
        this.processedAt = processedAt;
        this.createdAt = createdAt;
    }

    @Transient
    private List<NotificationRequestRecipientEntity> recipients;

    public void setRecipients(List<NotificationRequestRecipientEntity> recipients) {
        this.recipients = recipients;
    }

    @Transient
    private List<NotificationRequestSenderEntity> senders;

    public void setSenders(List<NotificationRequestSenderEntity> senders) {
        this.senders = senders;
    }

    @Transient
    private NotificationRequestContentEntity content;

    public void setContent(NotificationRequestContentEntity content) {
        this.content = content;
    }

    @Transient
    private NotificationRequestTemplateInfoEntity templateInfo;

    public void setTemplateInfo(NotificationRequestTemplateInfoEntity templateInfo) {
        this.templateInfo = templateInfo;
    }

    @Override
    @Nullable
    public String getId() {
        return this.requestId;
    }

    @Override
    public boolean isNew() {
        // 새 엔티티인지 여부를 판단하기 위해 createdAt이 null인지 확인
        boolean isNew = this.createdAt == null;
        if (isNew) {
            // 새 엔티티인 경우 createdAt을 현재 시간으로 설정
            // this.createdAt = LocalDateTime.now();
            this.createdAt = InstantDateTimeBridge.toLocalDateTime(Instant.now());
        }
        return isNew;
    }

}
