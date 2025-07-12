package notification.adapter.db;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import notification.domain.vo.recipient.AllUserRecipient;
import notification.domain.vo.recipient.DirectRecipient;
import notification.domain.vo.recipient.RecipientReference;
import notification.domain.vo.recipient.SegmentRecipient;
import notification.domain.vo.recipient.UserRecipient;

/**
 * 알림 요청의 수신자 정보를 저장하는 Entity
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notification_request_recipient")
public class NotificationRequestRecipientEntity implements Persistable<String> {

    @Id
    @Column("recipient_id")
    private String recipientId;

    @Column("request_id")
    private String requestId;

    @Column("recipient_type")
    private String recipientType; // USER, DIRECT, ALL_USER, SEGMENT

    @Column("user_id")
    private String userId;

    @Column("email_address")
    private String emailAddress;

    @Column("phone_number")
    private String phoneNumber;

    @Column("device_token")
    private String deviceToken;

    @Column("segment_name")
    private String segmentName;

    // 새 엔티티 여부를 판단하기 위한 필드
    @Transient
    @Default
    private boolean isNewEntity = false;

    public void markAsNew() {
        this.recipientId = UUID.randomUUID().toString();
        this.isNewEntity = true;
    }

    /**
     * RecipientReference 도메인 객체를 NotificationRequestRecipientEntity로 변환
     *
     * @param recipient   수신자 정보
     * @param requestId   요청 ID
     * @param recipientId 선택적 수신자 ID (null일 경우 UUID 생성)
     * @return 변환된 Entity
     */
    public static NotificationRequestRecipientEntity fromDomain(
            RecipientReference recipient, String requestId, String recipientId) {

        NotificationRequestRecipientEntityBuilder entity = NotificationRequestRecipientEntity.builder()
                .requestId(requestId)
                .recipientId(recipient.getId())
                .recipientType(recipient.getType().name());

        if (recipient instanceof UserRecipient userRecipient) {
            return entity.userId(userRecipient.userId().value())
                    .build();
        } else if (recipient instanceof DirectRecipient directRecipient) {
            return entity.emailAddress(directRecipient.emailAddress())
                    .phoneNumber(directRecipient.phoneNumber())
                    .deviceToken(directRecipient.deviceToken())
                    .build();
        } else if (recipient instanceof AllUserRecipient allUserRecipient) {
            return entity.build();
        } else if (recipient instanceof SegmentRecipient segmentRecipient) {
            return entity.segmentName(segmentRecipient.segmentName())
                    .build();
        }
        throw new IllegalArgumentException("Unsupported recipient type: " + recipient.getClass().getSimpleName());
    }

    @Override
    @Nullable
    public String getId() {
        return recipientId;
    }

    @Override
    public boolean isNew() {
        return isNewEntity;
    }

}
