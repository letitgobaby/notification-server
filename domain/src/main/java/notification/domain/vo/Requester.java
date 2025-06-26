package notification.domain.vo;

import java.util.Objects;

import notification.definition.annotations.ValueObject;
import notification.definition.enums.RequesterType;
import notification.definition.exceptions.PolicyViolationException;
import notification.definition.vo.UserId;

@ValueObject
public record Requester(RequesterType type, String id) {
    public Requester {
        Objects.requireNonNull(type, "Requester type cannot be null");
        Objects.requireNonNull(id, "Requester ID cannot be null");
        if (id.isBlank()) {
            throw new PolicyViolationException("Requester ID cannot be blank");
        }
    }

    public static Requester forUser(UserId userId) {
        Objects.requireNonNull(userId);
        return new Requester(RequesterType.USER, userId.value());
    }

    public static Requester forService(String serviceName) {
        Objects.requireNonNull(serviceName, "Service name cannot be null");
        return new Requester(RequesterType.SERVICE, serviceName);
    }

    public static Requester forAdmin(String adminId) {
        Objects.requireNonNull(adminId, "Admin ID cannot be null");
        return new Requester(RequesterType.ADMIN, adminId);
    }
}
