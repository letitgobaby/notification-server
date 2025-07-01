package notification.adapter.db;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Table("notification_message")
public class NotificationMessageEntity implements Persistable<String> {

    @Id
    @Column("message_id")
    private String messageId;

    @Column("request_id")
    private String requestId;

    @Column("notification_type")
    private String notificationType;

    // 수신자 정보
    @Column("user_id")
    private String userId;

    @Column("phone_number")
    private String phoneNumber;

    @Column("email")
    private String email;

    @Column("device_token")
    private String deviceToken;

    @Column("language")
    private String language;

    // 발신자 정보
    @Column("sender_phone_number")
    private String senderPhoneNumber;

    @Column("sender_email_address")
    private String senderEmailAddress;

    @Column("sender_name")
    private String senderName;

    // 알림 내용
    @Column("title")
    private String title;

    @Column("body")
    private String body;

    @Column("redirect_url")
    private String redirectUrl;

    @Column("image_url")
    private String imageUrl;

    // 상태 및 시간 정보
    @Column("delivery_status")
    private String deliveryStatus;

    @Column("scheduled_at")
    private Instant scheduledAt;

    @Column("dispatched_at")
    private Instant dispatchedAt;

    @Column("failure_reason")
    private String failureReason;

    @Column("created_at")
    private Instant createdAt;

    @Override
    @Nullable
    public String getId() {
        return this.messageId;
    }

    @Override
    public boolean isNew() {
        return this.createdAt == null; // 새로 생성된 경우 createdAt이 null
    }

}