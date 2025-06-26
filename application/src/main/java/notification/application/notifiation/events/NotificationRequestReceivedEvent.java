package notification.application.notifiation.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import notification.domain.RequestOutboxMessage;

@Getter
public class NotificationRequestReceivedEvent extends ApplicationEvent {

    private final RequestOutboxMessage outboxMessage;

    public NotificationRequestReceivedEvent(Object source, RequestOutboxMessage outboxMessage) {
        super(source);
        this.outboxMessage = outboxMessage;
    }
}
