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

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notification_request_template_info")
public class NotificationRequestTemplateInfoEntity implements Persistable<String> {

    @Id
    @Column("template_info_id")
    private String templateInfoId; // UUID로 생성된 고유 ID

    @Column("request_id")
    private String requestId; // FK로 저장된 NotificationRequest ID

    @Column("template_id")
    private String templateId;

    @Column("parameters")
    private String templateParameters; // JSON serialized Map<String, String>

    @Transient
    @Default
    private boolean isNewEntity = false;

    public void markAsNew() {
        this.templateInfoId = UUID.randomUUID().toString();
        this.isNewEntity = true;
    }

    /**
     * Value Object를 Entity로 변환합니다.
     *
     * @param templateId         템플릿 ID
     * @param templateParameters 템플릿 파라미터 (JSON 문자열)
     * @param requestId          요청 ID
     * @param templateInfoId     상위에서 받아온 PK
     * @return 변환된 Entity
     */
    public static NotificationRequestTemplateInfoEntity fromDomain(
            // TemplateInfo template, String requestId) {
            String templateInfoId, String templateId, String templateParameters, String requestId) {

        return NotificationRequestTemplateInfoEntity.builder()
                .templateInfoId(templateInfoId) // 상위에서 받아온 PK
                .requestId(requestId)
                .templateId(templateId)
                .templateParameters(templateParameters)
                .build();
    }

    @Override
    @Nullable
    public String getId() {
        return this.templateInfoId;
    }

    @Override
    public boolean isNew() {
        return this.isNewEntity;
    }

}
