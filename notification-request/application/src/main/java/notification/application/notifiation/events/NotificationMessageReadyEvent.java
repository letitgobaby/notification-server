package notification.application.notifiation.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import notification.definition.vo.outbox.MessageOutbox;

@Getter
public class NotificationMessageReadyEvent extends ApplicationEvent {

    private final MessageOutbox MessageOutbox;

    public NotificationMessageReadyEvent(Object source, MessageOutbox MessageOutbox) {
        super(source);
        this.MessageOutbox = MessageOutbox;
    }

}
