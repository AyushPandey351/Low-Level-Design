package notification;

import java.util.concurrent.atomic.AtomicInteger;

// Test-only decorator: wraps a real channel and fails the first N attempts before
// delegating through successfully. This exists purely so Main can deterministically
// exercise RetryService's exponential backoff and eventual-success path - without
// it, there'd be no way to observe a retry actually happening, since the real
// channel implementations always "succeed." Implemented as a decorator (wrapping a
// delegate NotificationChannel) rather than a boolean parameter bolted onto the
// interface, so the interface itself stays exactly `send(Notification)`, uncluttered
// by test-only concerns - the same reasoning BookMyShow's PaymentService used a
// `simulateSuccess` parameter, just applied via composition instead here since
// NotificationChannel has no natural place for such a flag without changing its
// one-method contract.
public class FlakyChannelWrapper implements NotificationChannel {
    private final NotificationChannel delegate;
    private final AtomicInteger failuresRemaining;

    public FlakyChannelWrapper(NotificationChannel delegate, int failuresBeforeSuccess) {
        this.delegate = delegate;
        this.failuresRemaining = new AtomicInteger(failuresBeforeSuccess);
    }

    @Override
    public void send(Notification notification) {
        if (failuresRemaining.getAndDecrement() > 0) {
            throw new NotificationDeliveryException("Simulated transient failure for " + notification.getNotificationId());
        }
        delegate.send(notification);
    }
}
