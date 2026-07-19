package ring;

import java.util.concurrent.atomic.AtomicInteger;

// Test-only decorator, same shape as FlakyChannelWrapper in the Notification System
// design - wraps a real provider and fails a configurable number of times before
// delegating through, purely so Main can deterministically exercise
// RetryService's exponential backoff and DLQ routing.
public class FlakyPushProvider implements PushProvider {
    private final PushProvider delegate;
    private final AtomicInteger failuresRemaining;

    public FlakyPushProvider(PushProvider delegate, int failuresBeforeSuccess) {
        this.delegate = delegate;
        this.failuresRemaining = new AtomicInteger(failuresBeforeSuccess);
    }

    @Override
    public void send(Notification notification, Device device) {
        if (failuresRemaining.getAndDecrement() > 0) {
            throw new PushDeliveryException("Simulated provider failure for " + notification.getNotificationId());
        }
        delegate.send(notification, device);
    }
}
