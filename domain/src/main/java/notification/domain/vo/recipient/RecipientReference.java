package notification.domain.vo.recipient;

public sealed interface RecipientReference
        permits UserRecipient, DirectRecipient, AllUserRecipient, SegmentRecipient {
}