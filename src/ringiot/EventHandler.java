package ringiot;

@FunctionalInterface
public interface EventHandler {
    void handle(Event event);
}
