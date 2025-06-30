package notification.domain.vo;

import notification.definition.annotations.ValueObject;
import notification.domain.enums.RequesterType;

@ValueObject
public record Requester(RequesterType type, String id) {
}
