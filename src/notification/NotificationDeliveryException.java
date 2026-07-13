package notification;

// Thrown by a NotificationChannel.send() to signal a delivery failure - distinct
// from a generic RuntimeException so RetryService can specifically catch "the
// provider rejected/failed this send" without accidentally swallowing unrelated bugs.
public class NotificationDeliveryException extends RuntimeException {
    public NotificationDeliveryException(String message) {
        super(message);
    }
}
