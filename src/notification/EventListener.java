package notification;

// Observer Pattern, generic over the event type so the same publish/subscribe
// machinery works for OrderPlacedEvent here and any other domain event later
// (PaymentFailedEvent, ShipmentDispatchedEvent, ...) without new infrastructure.
public interface EventListener<T> {
    void onEvent(T event);
}
