package notification.domain.vo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import notification.definition.annotations.ValueObject;
import notification.definition.enums.AudienceType;
import notification.definition.exceptions.PolicyViolationException;
import notification.definition.vo.UserId;

@ValueObject
public record TargetAudience(
        AudienceType type,
        Set<UserId> userIds, // 사용자 ID 집합 (SINGLE_USER 또는 MULTIPLE_USERS일 때 사용)
        String segmentName, // 특정 세그먼트 이름
        List<Recipient> directRecipients // 직접 수신자 목록
) {

    public TargetAudience {
        // 유효성 검사
        if (type == null) {
            throw new PolicyViolationException("Audience type cannot be null");
        }

        switch (type) {
            case SINGLE_USER, MULTIPLE_USERS -> {
                if (userIds == null || userIds.isEmpty()) {
                    throw new PolicyViolationException("User IDs must be provided for MULTIPLE_USERS or SINGLE_USER");
                }
                if (type == AudienceType.SINGLE_USER && userIds.size() != 1) {
                    throw new PolicyViolationException("SINGLE_USER audience must have exactly one user ID");
                }
            }
            case SEGMENT -> {
                if (segmentName == null || segmentName.isBlank()) {
                    throw new PolicyViolationException("Segment name must be provided for SEGMENT audience");
                }
            }
            case DIRECT_RECIPIENTS -> {
                if (directRecipients == null || directRecipients.isEmpty()) {
                    throw new PolicyViolationException(
                            "Direct recipients must be provided for DIRECT_RECIPIENTS audience");
                }
            }
            case MIXED -> {
                boolean hasUserIds = userIds != null && !userIds.isEmpty();
                boolean hasDirectRecipients = directRecipients != null && !directRecipients.isEmpty();
                if (!hasUserIds && !hasDirectRecipients) {
                    throw new PolicyViolationException(
                            "MIXED audience must have either userIds or directRecipients (or both).");
                }
            }
            case ALL_USERS -> {
            }
        }

        userIds = Collections.unmodifiableSet(userIds);
        directRecipients = Collections.unmodifiableList(directRecipients);
        segmentName = segmentName != null ? segmentName.trim() : null;
    }

    /**
     * 하나의 사용자에게 알림을 전송
     * 
     * @param userId
     * @return
     */
    public static TargetAudience singleUser(UserId userId) {
        return new TargetAudience(AudienceType.SINGLE_USER, Set.of(userId), null, Collections.emptyList());
    }

    /**
     * 여러 사용자에게 알림을 전송
     * 
     * @param userIds
     * @return
     */
    public static TargetAudience multipleUsers(Set<UserId> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new PolicyViolationException("User IDs cannot be null or empty for multiple users.");
        }

        if (userIds.size() == 1) {
            return singleUser(userIds.iterator().next());
        }

        return new TargetAudience(AudienceType.MULTIPLE_USERS, userIds, null, Collections.emptyList());
    }

    /**
     * 모든 사용자에게 알림을 전송
     * 
     * @return
     */
    public static TargetAudience allUsers() {
        return new TargetAudience(AudienceType.ALL_USERS, Collections.emptySet(), null, Collections.emptyList());
    }

    /**
     * 특정 세그먼트에 속한 사용자에게 알림을 전송
     * 
     * @param segmentName
     * @return
     */
    public static TargetAudience bySegment(String segmentName) {
        return new TargetAudience(AudienceType.SEGMENT, Collections.emptySet(), segmentName, Collections.emptyList());
    }

    /**
     * 직접 수신자 목록에 알림을 전송
     * 
     * @param recipients
     * @return
     */
    public static TargetAudience directRecipients(List<Recipient> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("Recipients list cannot be empty for direct recipients.");
        }

        return new TargetAudience(AudienceType.DIRECT_RECIPIENTS, Collections.emptySet(), null, recipients);
    }

    /**
     * 혼합된 대상 (사용자 ID와 직접 수신자 목록을 모두 포함할 수 있음)
     * 
     * @param userIds
     * @param directRecipients
     * @return
     */
    public static TargetAudience mixedAudience(Set<UserId> userIds, List<Recipient> directRecipients) {
        boolean hasUserIds = userIds != null && !userIds.isEmpty();
        boolean hasDirectRecipients = directRecipients != null && !directRecipients.isEmpty();
        if (!hasUserIds && !hasDirectRecipients) {
            throw new IllegalArgumentException(
                    "MIXED audience must have either userIds or directRecipients (or both).");
        }

        return new TargetAudience(AudienceType.MIXED, userIds, null, directRecipients);
    }

}
