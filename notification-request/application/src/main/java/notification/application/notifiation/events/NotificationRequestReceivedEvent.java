package notification.application.notifiation.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import notification.definition.vo.outbox.RequestOutbox;

@Getter
public class NotificationRequestReceivedEvent extends ApplicationEvent {

    private final RequestOutbox requestOutbox;

    public NotificationRequestReceivedEvent(Object source, RequestOutbox requestOutbox) {
        super(source);
        this.requestOutbox = requestOutbox;
    }
}
