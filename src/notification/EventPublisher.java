package notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// The reusable pub/sub bus itself. This is the concrete answer to "how does Order
// Service publish an event without knowing NotificationService exists" - Order
// Service (or whatever OrderService stands in for here) only ever calls publish()
// on its OWN EventPublisher instance; it has zero reference to NotificationService,
// EmailNotificationChannel, or anything else downstream. NotificationService
// subscribes ITSELF to whichever publishers it cares about - the dependency points
// from listener to publisher, never the other way, which is the entire decoupling
// benefit of Observer/Pub-Sub.
//
// CopyOnWriteArrayList because subscription lists are read far more often (every
// publish() iterates them) than written (subscribe() calls are rare, typically only
// at startup) - the classic case that pattern is built for, and it avoids needing
// separate synchronization for a list that's rarely mutated but frequently iterated.
public class EventPublisher<T> {
    private final List<EventListener<T>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(EventListener<T> listener) {
        listeners.add(listener);
    }

    public void publish(T event) {
        for (EventListener<T> listener : listeners) {
            listener.onEvent(event);
        }
    }
}
