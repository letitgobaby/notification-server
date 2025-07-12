package notification.adapter.db;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

import lombok.Builder;
import lombok.Getter;

@Getter
// @Builder
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
    @Column("sender_id")
    private String senderId;

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
    private LocalDateTime scheduledAt;

    @Column("dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column("failure_reason")
    private String failureReason;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Builder
    public NotificationMessageEntity(String messageId, String requestId, String notificationType,
            String userId, String phoneNumber, String email,
            String deviceToken, String language,
            String senderId, String senderPhoneNumber,
            String senderEmailAddress, String senderName,
            String title, String body, String redirectUrl,
            String imageUrl, String deliveryStatus,
            LocalDateTime scheduledAt, LocalDateTime dispatchedAt,
            String failureReason, LocalDateTime createdAt) {
        this.messageId = messageId;
        this.requestId = requestId;
        this.notificationType = notificationType;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.deviceToken = deviceToken;
        this.language = language;
        this.senderId = senderId;
        this.senderPhoneNumber = senderPhoneNumber;
        this.senderEmailAddress = senderEmailAddress;
        this.senderName = senderName;
        this.title = title;
        this.body = body;
        this.redirectUrl = redirectUrl;
        this.imageUrl = imageUrl;
        this.deliveryStatus = deliveryStatus;
        this.scheduledAt = scheduledAt;
        this.dispatchedAt = dispatchedAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    @Override
    @Nullable
    public String getId() {
        return this.messageId;
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