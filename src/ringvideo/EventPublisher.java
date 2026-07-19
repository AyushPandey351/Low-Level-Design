package ringvideo;

import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.CopyOnWriteArrayList;

// Publisher-Subscriber from the notes' pattern table ("S3 event -> queue ->
// processing workers"). Uses java.util.function.Consumer<T> as the subscriber type
// directly rather than a custom listener interface (this series' third time
// building this exact pub-sub shape - Notification System's EventPublisher, Ring
// IoT's EventBus - Consumer<T> is the standard-library equivalent and saves one
// more near-identical interface file for no real benefit this time around).
public class EventPublisher<T> {
    private final List<Consumer<T>> subscribers = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<T> subscriber) {
        subscribers.add(subscriber);
    }

    public void publish(T event) {
        for (Consumer<T> subscriber : subscribers) {
            subscriber.accept(event);
        }
    }
}
