package ring;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Implements the notes' "1s -> 5s -> 30s" backoff schedule, scaled down (same ratio
// of growth, shorter absolute durations) purely so this design's demo finishes in a
// reasonable time - the point being demonstrated is the GROWTH pattern, not the
// specific starting duration, which would be tuned per real push provider in
// production.
//
// Retries run on a ScheduledExecutorService rather than blocking the calling thread
// with Thread.sleep (unlike RetryService in the earlier Notification System design,
// which DID block) - here that distinction matters more, because the caller is a
// NotificationWorker thread that owns an entire partition's ordering (see
// PartitionedNotificationQueue). Blocking that thread during a retry backoff would
// stall every OTHER event queued behind it for the SAME doorbell, even though those
// events have nothing to do with the failed delivery. Scheduling the retry
// asynchronously lets the worker move on to the next queued event immediately.
public class ExponentialBackoffRetryService implements RetryService {
    private static final int MAX_RETRIES = 3;
    private static final long[] BACKOFF_MILLIS = {200, 1000, 3000};

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final List<Notification> deadLetterQueue = new CopyOnWriteArrayList<>();
    private final NotificationRepository notificationRepository;

    public ExponentialBackoffRetryService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void scheduleRetry(Notification notification, Device device, PushProvider provider) {
        int attempt = notification.incrementAndGetRetryCount();
        if (attempt > MAX_RETRIES) {
            moveToDLQ(notification);
            return;
        }
        long delay = BACKOFF_MILLIS[Math.min(attempt - 1, BACKOFF_MILLIS.length - 1)];
        System.out.println("[RetryService] Retry " + attempt + " for " + notification.getNotificationId() + " in " + delay + "ms");
        scheduler.schedule(() -> attemptSend(notification, device, provider), delay, TimeUnit.MILLISECONDS);
    }

    private void attemptSend(Notification notification, Device device, PushProvider provider) {
        try {
            provider.send(notification, device);
            notification.markSent();
            notificationRepository.update(notification);
        } catch (PushDeliveryException e) {
            scheduleRetry(notification, device, provider);
        }
    }

    @Override
    public void moveToDLQ(Notification notification) {
        notification.markDeadLettered();
        notificationRepository.update(notification);
        deadLetterQueue.add(notification);
        System.out.println("[RetryService] " + notification.getNotificationId() + " moved to DLQ after exceeding retry limit");
    }

    @Override
    public List<Notification> getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
