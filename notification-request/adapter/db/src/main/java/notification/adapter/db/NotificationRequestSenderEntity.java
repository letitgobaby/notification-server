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
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SenderInfo;
import notification.domain.vo.sender.SmsSender;

/**
 * 알림 요청의 발신자 정보를 저장하는 Entity
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notification_request_sender")
public class NotificationRequestSenderEntity implements Persistable<String> {

    @Id
    @Column("sender_id")
    private String senderId;

    @Column("request_id")
    private String requestId;

    @Column("notification_type")
    private String notificationType; // EMAIL, SMS, PUSH, etc.

    @Column("sender_name")
    private String senderName;

    @Column("sender_email")
    private String senderEmail;

    @Column("sender_phone")
    private String senderPhone;

    @Transient
    @Default
    private boolean isNewEntity = false;

    public void markAsNew() {
        this.senderId = UUID.randomUUID().toString();
        this.isNewEntity = true;
    }

    /**
     * SenderInfo 도메인 객체를 NotificationRequestSenderEntity로 변환합니다.
     *
     * @param senderInfo 발신자 정보
     * @param requestId  요청 ID
     * @return 변환된 NotificationRequestSenderEntity
     */
    public static NotificationRequestSenderEntity fromDomain(SenderInfo senderInfo, String requestId) {
        NotificationRequestSenderEntityBuilder entity = NotificationRequestSenderEntity.builder()
                .requestId(requestId)
                .notificationType(senderInfo.getType().name())
                .senderId(senderInfo.getId());

        if (senderInfo instanceof EmailSender emailSender) {
            return entity.senderName(emailSender.senderName())
                    .senderEmail(emailSender.senderEmailAddress())
                    .build();
        } else if (senderInfo instanceof PushSender pushSender) {
            return entity.senderName(pushSender.senderName())
                    .build();
        } else if (senderInfo instanceof SmsSender smsSender) {
            return entity.senderName(smsSender.senderName())
                    .senderPhone(smsSender.senderPhoneNumber())
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported sender type: "
                    + senderInfo.getClass().getSimpleName());
        }
    }

    @Override
    @Nullable
    public String getId() {
        return senderId;
    }

    @Override
    public boolean isNew() {
        return isNewEntity;
    }

}
