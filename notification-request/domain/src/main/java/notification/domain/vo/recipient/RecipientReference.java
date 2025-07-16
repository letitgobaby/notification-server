package notification.domain.vo.recipient;

import notification.domain.enums.RecipientType;

public sealed interface RecipientReference
        permits UserRecipient, DirectRecipient, AllUserRecipient, SegmentRecipient {

    String getId();

    RecipientType getType();
}