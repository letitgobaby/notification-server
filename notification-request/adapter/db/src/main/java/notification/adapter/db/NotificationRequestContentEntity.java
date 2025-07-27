package notification.adapter.db;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.micrometer.common.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import notification.domain.vo.NotificationContent;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notification_request_content")
public class NotificationRequestContentEntity implements Persistable<String> {

    @Id
    @Column("content_id")
    private String contentId;

    @Column("request_id")
    private String requestId;

    @Column("title")
    private String title;

    @Column("body")
    private String body;

    @Column("image_url")
    private String imageUrl;

    @Column("redirect_url")
    private String redirectUrl;

    @Transient
    @Default
    private boolean isNewEntity = false;

    public void markAsNew() {
        this.contentId = UUID.randomUUID().toString();
        this.isNewEntity = true;
    }

    /**
     * Value Object를 Entity로 변환합니다.
     *
     * @param content   변환할 Value Object
     * @param contentId 상위에서 받아온 PK
     * @param requestId 요청 ID
     * @return 변환된 Entity
     */
    public static NotificationRequestContentEntity fromDomain(
            NotificationContent content, String requestId) {
        return NotificationRequestContentEntity.builder()
                .contentId(content.getContentId()) // 상위에서 받아온 PK
                .requestId(requestId)
                .title(content.getTitle())
                .body(content.getBody())
                .imageUrl(content.getImageUrl())
                .redirectUrl(content.getRedirectUrl())
                .build();
    }

    /**
     * Entity를 Value Object로 변환합니다.
     *
     * @param entity 변환할 Entity
     * @return 변환된 Value Object
     */
    public NotificationContent toValueObject() {
        return new NotificationContent(
                this.contentId,
                this.title, this.body, this.redirectUrl, this.imageUrl);
    }

    @Override
    @Nullable
    public String getId() {
        return this.contentId;
    }

    @Override
    public boolean isNew() {
        return this.isNewEntity;
    }

}
