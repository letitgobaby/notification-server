package notification.domain.vo.recipient;

import lombok.Getter;
import lombok.experimental.Accessors;
import notification.definition.annotations.ValueObject;
import notification.domain.enums.RecipientType;

@Getter
@Accessors(fluent = true)
@ValueObject
public final class SegmentRecipient implements RecipientReference {

    private final String recipientId;
    private final String segmentName;

    public SegmentRecipient(String recipientId, String segmentName) {
        this.recipientId = recipientId;
        this.segmentName = segmentName;
    }

    public SegmentRecipient(String segmentName) {
        this.recipientId = null;
        this.segmentName = segmentName;
    }

    @Override
    public String getId() {
        return recipientId;
    }

    @Override
    public RecipientType getType() {
        return RecipientType.SEGMENT;
    }
}