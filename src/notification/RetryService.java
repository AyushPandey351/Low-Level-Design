package notification;

// Implements Step 8's "Retry with Exponential Backoff": 1s, 2s, 4s... instead of
// hammering a struggling provider at a constant interval, which is exactly what
// "prevents overloading providers" means in practice - a struggling email/SMS
// provider getting immediately retried at full speed by every failed send is more
// likely to make an outage WORSE, not better.
//
// INITIAL_BACKOFF_MILLIS is intentionally short (200ms, not a real 1000ms+) purely
// so this design's demo runs in a reasonable time - the DOUBLING behavior is what's
// being demonstrated, not the specific starting duration, which would be tuned per
// real provider in production.
public class RetryService {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 200;

    public void sendWithRetry(Notification notification, NotificationChannel channel) {
        long backoff = INITIAL_BACKOFF_MILLIS;

        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                channel.send(notification);
                notification.markSent();
                return;
            } catch (NotificationDeliveryException e) {
                if (attempt > MAX_RETRIES) {
                    notification.markFailed();
                    System.out.println("[RetryService] " + notification.getNotificationId()
                            + " permanently FAILED after " + MAX_RETRIES + " retries: " + e.getMessage());
                    return;
                }
                notification.markRetrying();
                System.out.println("[RetryService] " + notification.getNotificationId()
                        + " attempt " + attempt + " failed (" + e.getMessage() + "), retrying in " + backoff + "ms");
                sleep(backoff);
                backoff *= 2;
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
