package notification;

// A minimal domain event - just enough data for a listener to act on. Deliberately
// NOT part of the notification domain itself (it doesn't reference Notification,
// ChannelType, or anything notification-specific) - it belongs conceptually to the
// order domain, and NotificationService is just one of potentially several
// consumers reacting to it.
public class OrderPlacedEvent {
    private final String userId;
    private final String orderId;
    private final double amount;

    public OrderPlacedEvent(String userId, String orderId, double amount) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getAmount() {
        return amount;
    }
}
