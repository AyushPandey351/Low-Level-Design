package ring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryNotificationRepository implements NotificationRepository {
    private final Map<String, Notification> notifications = new ConcurrentHashMap<>();

    @Override
    public void save(Notification notification) {
        notifications.put(notification.getNotificationId(), notification);
    }

    @Override
    public void update(Notification notification) {
        notifications.put(notification.getNotificationId(), notification);
    }

    @Override
    public Notification find(String notificationId) {
        return notifications.get(notificationId);
    }
}
