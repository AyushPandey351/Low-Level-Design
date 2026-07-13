package notification;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// ConcurrentHashMap for the same reason as every repository earlier in this series
// (PaymentRepository, CalendarRepository) - this system explicitly must "handle
// concurrent notification requests."
//
// markProcessedIfNew() is Step 8's "Idempotency" fix: if a worker crashes right
// after actually sending an email but before recording success, and the surrounding
// system retries the whole job, this is what prevents the user getting the same
// email twice. `Set.add()` on a ConcurrentHashMap-backed set is atomic - the same
// "atomically reserve, act only if you were first" idiom as
// PaymentRepository.reserveIdempotencyKey's putIfAbsent, just via a Set instead of a
// Map since there's no second value to associate, only a yes/no "have we seen this
// id before."
public class NotificationRepository {
    private final Map<String, Notification> notifications = new ConcurrentHashMap<>();
    private final Set<String> processedIds = ConcurrentHashMap.newKeySet();

    public void save(Notification notification) {
        notifications.put(notification.getNotificationId(), notification);
    }

    public Notification find(String notificationId) {
        return notifications.get(notificationId);
    }

    public void update(Notification notification) {
        notifications.put(notification.getNotificationId(), notification);
    }

    // Returns true only the FIRST time a given notificationId is seen - any later
    // call for the same id returns false, telling the caller "skip it, already handled."
    public boolean markProcessedIfNew(String notificationId) {
        return processedIds.add(notificationId);
    }
}
