package notification.adapter.db;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("notification_request")
public class NotificationRequestEntity {

    @Id
    @Column("request_id")
    private String requestId;

    @Column("requester_type")
    private String requesterType;

    @Column("requester_id")
    private String requesterId;

    @Column("audience_type")
    private String audienceType;

    @Column("user_ids")
    private String userIds; // JSON or comma-separated

    @Column("segment_name")
    private String segmentName;

    @Column("direct_recipients")
    private String directRecipients; // JSON

    @Column("use_template")
    private Boolean useTemplate;

    @Column("template_id")
    private String templateId;

    @Column("template_parameters")
    private String templateParameters; // JSON

    @Column("direct_content")
    private String directContent; // JSON

    @Column("channel_configs")
    private String channelConfigs; // JSON

    @Column("scheduled_at")
    private Instant scheduledAt;

    @Column("requested_at")
    private Instant requestedAt;

    @Column("status")
    private String status;

    @Column("failure_reason")
    private String failureReason;

    @Column("processed_at")
    private Instant processedAt;

}
