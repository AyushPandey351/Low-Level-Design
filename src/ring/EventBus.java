package ring;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Simulates the SNS topic from the notes: "one event may have multiple consumers
// (push notification, video recording, AI detection, audit logs, analytics)...
// publish-subscribe fan-out without coupling producers to consumers." The Doorbell
// Gateway publishes ONE DoorbellEvent here and has zero references to whatever
// consumes it - Main wires up a notification consumer, an analytics consumer, and a
// video consumer, none of which know the others exist either.
public class EventBus<T> {
    private final List<EventSubscriber<T>> subscribers = new CopyOnWriteArrayList<>();

    public void subscribe(EventSubscriber<T> subscriber) {
        subscribers.add(subscriber);
    }

    public void publish(T event) {
        for (EventSubscriber<T> subscriber : subscribers) {
            subscriber.onEvent(event);
        }
    }
}
