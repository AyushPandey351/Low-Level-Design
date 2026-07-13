package notification;

// A thin stand-in for a real, separate Order service - just enough to prove the
// Observer/Pub-Sub point from your Step 4 and Step 6 notes: this class publishes
// OrderPlacedEvent and has ABSOLUTELY NO knowledge of NotificationService, Email,
// SMS, or Push. It doesn't import them, doesn't call them, doesn't even know
// anyone is listening. Whoever subscribes to its EventPublisher decides what
// happens next - in a real distributed system, `publisher` here would be replaced
// by a Kafka/SQS topic, but the DECOUPLING relationship is identical.
public class OrderService {
    private final EventPublisher<OrderPlacedEvent> publisher = new EventPublisher<>();

    public void subscribe(EventListener<OrderPlacedEvent> listener) {
        publisher.subscribe(listener);
    }

    public void placeOrder(String userId, String orderId, double amount) {
        System.out.println("[OrderService] Order " + orderId + " placed by " + userId + " for " + amount);
        publisher.publish(new OrderPlacedEvent(userId, orderId, amount));
    }
}
