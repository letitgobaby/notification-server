package notification.domain.vo.recipient;

import notification.definition.annotations.ValueObject;

@ValueObject
public record SegmentRecipient(String segmentName) implements RecipientReference {
}
