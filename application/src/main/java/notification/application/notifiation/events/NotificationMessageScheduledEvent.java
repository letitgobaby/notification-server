package notification.application.notifiation.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import notification.domain.OutboxMessage;

@Getter
public class NotificationMessageScheduledEvent extends ApplicationEvent {

    private final OutboxMessage outboxMessage;

    public NotificationMessageScheduledEvent(Object source, OutboxMessage outboxMessage) {
        super(source);
        this.outboxMessage = outboxMessage;
    }

}
