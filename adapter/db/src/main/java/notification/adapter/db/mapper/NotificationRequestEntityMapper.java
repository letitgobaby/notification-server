package notification.adapter.db.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notification.adapter.db.NotificationRequestContentEntity;
import notification.adapter.db.NotificationRequestEntity;
import notification.adapter.db.NotificationRequestRecipientEntity;
import notification.adapter.db.NotificationRequestSenderEntity;
import notification.adapter.db.NotificationRequestTemplateInfoEntity;
import notification.definition.exceptions.ObjectConversionException;
import notification.definition.utils.InstantDateTimeBridge;
import notification.domain.NotificationRequest;
import notification.domain.enums.NotificationType;
import notification.domain.enums.RecipientType;
import notification.domain.enums.RequestStatus;
import notification.domain.enums.RequesterType;
import notification.domain.vo.NotificationContent;
import notification.domain.vo.NotificationRequestId;
import notification.domain.vo.Requester;
import notification.domain.vo.TemplateInfo;
import notification.domain.vo.UserId;
import notification.domain.vo.recipient.AllUserRecipient;
import notification.domain.vo.recipient.DirectRecipient;
import notification.domain.vo.recipient.RecipientReference;
import notification.domain.vo.recipient.SegmentRecipient;
import notification.domain.vo.recipient.UserRecipient;
import notification.domain.vo.sender.EmailSender;
import notification.domain.vo.sender.PushSender;
import notification.domain.vo.sender.SenderInfo;
import notification.domain.vo.sender.SmsSender;

/**
 * NotificationRequest 도메인 객체와 Entity 간의 변환을 담당하는 매퍼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestEntityMapper {

    private final ObjectMapper objectMapper;

    /**
     * NotificationRequestEntity를 NotificationRequest 도메인 객체로 변환합니다.
     *
     * @param entity 변환할 Entity
     * @return 변환된 도메인 객체
     */
    public NotificationRequest toDomain(NotificationRequestEntity entity) {
        return new NotificationRequest(
                new NotificationRequestId(entity.getRequestId()),
                new Requester(RequesterType.valueOf(entity.getRequesterType()), entity.getRequesterId()),
                toRecipientReferences(entity.getRecipients()),
                deserializeNotificationTypes(entity.getNotificationTypes()),
                toSenderInfoMap(entity.getSenders()),
                toContent(entity.getContent()),
                toTemplateInfo(entity.getTemplateInfo()),
                entity.getMemo(),
                InstantDateTimeBridge.toInstant(entity.getScheduledAt()),
                RequestStatus.valueOf(entity.getStatus()),
                entity.getFailureReason(),
                InstantDateTimeBridge.toInstant(entity.getProcessedAt()),
                InstantDateTimeBridge.toInstant(entity.getCreatedAt()));
    }

    private List<RecipientReference> toRecipientReferences(List<NotificationRequestRecipientEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream().map(entity -> {
            switch (RecipientType.valueOf(entity.getRecipientType())) {
                case USER:
                    return new UserRecipient(entity.getRecipientId(), new UserId(entity.getUserId()));
                case DIRECT:
                    return new DirectRecipient(
                            entity.getRecipientId(),
                            entity.getPhoneNumber(),
                            entity.getEmailAddress(),
                            entity.getDeviceToken());
                case ALL_USER:
                    return new AllUserRecipient(entity.getRecipientId());
                case SEGMENT:
                    return new SegmentRecipient(entity.getRecipientId(), entity.getSegmentName());
                default:
                    throw new IllegalArgumentException("Unknown recipient type: "
                            + entity.getRecipientType());
            }
        }).collect(Collectors.toList());
    }

    private Map<NotificationType, SenderInfo> toSenderInfoMap(List<NotificationRequestSenderEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Map.of();
        }

        return entities.stream().collect(Collectors.toMap(
                entity -> NotificationType.valueOf(entity.getNotificationType()),
                entity -> {
                    switch (NotificationType.valueOf(entity.getNotificationType())) {
                        case EMAIL:
                            return new EmailSender(entity.getSenderId(),
                                    entity.getSenderEmail(), entity.getSenderName());
                        case SMS:
                            return new SmsSender(entity.getSenderId(),
                                    entity.getSenderPhone(), entity.getSenderName());
                        case PUSH:
                            return new PushSender(entity.getSenderId(),
                                    entity.getSenderName());
                        default:
                            throw new IllegalArgumentException("Unknown notification type: "
                                    + entity.getNotificationType());
                    }
                }));
    }

    private NotificationContent toContent(NotificationRequestContentEntity entity) {
        if (entity == null) {
            return null;
        }

        return new NotificationContent(
                entity.getTitle(),
                entity.getBody(),
                entity.getRedirectUrl(),
                entity.getImageUrl());
    }

    private TemplateInfo toTemplateInfo(NotificationRequestTemplateInfoEntity entity) {
        if (entity == null) {
            return null;
        }

        return new TemplateInfo(
                entity.getTemplateId(),
                deserializeTemplateParameters(entity.getTemplateParameters()));
    }

    /**
     * NotificationRequest 도메인 객체를 NotificationRequestEntity로 변환합니다.
     *
     * @param domain 변환할 도메인 객체
     * @return 변환된 Entity
     */
    public NotificationRequestEntity toEntity(NotificationRequest domain) {
        return NotificationRequestEntity.builder()
                .requestId(domain.getRequestId().value())
                .requesterType(domain.getRequester().type().name())
                .requesterId(domain.getRequester().id())
                .notificationTypes(serializeNotificationTypes(domain.getNotificationTypes()))
                .memo(domain.getMemo())
                .scheduledAt(InstantDateTimeBridge.toLocalDateTime(domain.getScheduledAt()))
                .status(domain.getStatus().name())
                .failureReason(domain.getFailureReason())
                .processedAt(InstantDateTimeBridge.toLocalDateTime(domain.getProcessedAt()))
                .createdAt(InstantDateTimeBridge.toLocalDateTime(domain.getCreatedAt()))
                .build();
    }

    /**
     * NotificationRequest 도메인 객체의 수신자 목록을 NotificationRequestRecipientEntity 목록으로
     * 변환합니다.
     *
     * @param domain    변환할 도메인 객체
     * @param requestId 요청 ID
     * @return 변환된 Entity 목록
     */
    public List<NotificationRequestRecipientEntity> toRecipientEntities(NotificationRequest domain, String requestId) {
        if (domain.getRecipients() == null || domain.getRecipients().isEmpty()) {
            return List.of();
        }

        return domain.getRecipients().stream().map(recipient -> {
            var entity = NotificationRequestRecipientEntity.fromDomain(
                    recipient, requestId, recipient.getId());

            if (domain.getCreatedAt() == null) {
                entity.markAsNew();
            }

            return entity;
        }).collect(Collectors.toList());
    }

    /**
     * NotificationRequest 도메인 객체의 발신자 목록을 NotificationRequestSenderEntity 목록으로
     * 변환합니다.
     *
     * @param domain    변환할 도메인 객체
     * @param requestId 요청 ID
     * @return 변환된 Entity 목록
     */
    public List<NotificationRequestSenderEntity> toSenderEntities(NotificationRequest domain, String requestId) {
        if (domain.getSenderInfos() == null || domain.getSenderInfos().isEmpty()) {
            return List.of();
        }

        return domain.getSenderInfos().values().stream().map(senderInfo -> {
            var entity = NotificationRequestSenderEntity.fromDomain(
                    senderInfo, requestId);

            if (domain.getCreatedAt() == null) {
                entity.markAsNew();
            }

            return entity;
        }).collect(Collectors.toList());
    }

    /**
     * NotificationRequest 도메인 객체를 NotificationRequestContentEntity로 변환합니다.
     *
     * @param domain    변환할 도메인 객체
     * @param requestId 요청 ID
     * @param contentId 컨텐츠 ID
     * @return 변환된 Entity
     */
    public NotificationRequestContentEntity toContentEntity(NotificationRequest domain,
            String requestId, String contentId) {
        if (domain.getContent() == null) {
            return null;
        }

        var entity = NotificationRequestContentEntity.fromDomain(
                domain.getContent(), requestId, contentId);

        if (domain.getCreatedAt() == null || contentId == null) {
            entity.markAsNew();
        }

        return entity;
    }

    /**
     * NotificationRequest 도메인 객체를 NotificationRequestTemplateInfoEntity로 변환합니다.
     *
     * @param domain         변환할 도메인 객체
     * @param requestId      요청 ID
     * @param templateInfoId 템플릿 정보 ID
     * @return 변환된 Entity
     */
    public NotificationRequestTemplateInfoEntity toTemplateInfoEntity(NotificationRequest domain,
            String requestId, String templateInfoId) {
        if (domain.getTemplate() == null) {
            return null;
        }

        var entity = NotificationRequestTemplateInfoEntity.fromDomain(
                domain.getTemplate().templateId(),
                serializeTemplateParameters(domain.getTemplate().parameters()),
                requestId, templateInfoId);

        if (domain.getCreatedAt() == null || templateInfoId == null) {
            entity.markAsNew();
        }

        return entity;
    }

    /**
     * NotificationType 리스트를 JSON으로 직렬화
     */
    private String serializeNotificationTypes(List<NotificationType> types) {
        try {
            return objectMapper.writeValueAsString(types);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification types: {}", types, e);
            throw new ObjectConversionException("Failed to serialize notification types", e);
        }
    }

    /**
     * JSON을 NotificationType 리스트로 역직렬화
     */
    private List<NotificationType> deserializeNotificationTypes(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<NotificationType>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize notification types: {}", json, e);
            throw new ObjectConversionException("Failed to deserialize notification types", e);
        }
    }

    /**
     * 템플릿 매개변수를 JSON으로 직렬화
     */
    private String serializeTemplateParameters(Map<String, String> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize template parameters: {}", parameters, e);
            throw new ObjectConversionException("Failed to serialize template parameters", e);
        }
    }

    /**
     * JSON을 템플릿 매개변수 맵으로 역직렬화
     */
    private Map<String, String> deserializeTemplateParameters(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize template parameters: {}", json, e);
            throw new ObjectConversionException("Failed to deserialize template parameters", e);
        }
    }
}
