package ring;

public interface EventSubscriber<T> {
    void onEvent(T event);
}
