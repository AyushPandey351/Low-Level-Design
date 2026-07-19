package ring;

// Tracks delivery to ONE device - same "one record per delivery target" shape as
// Notification in the Notification System design, for the same reason: a single
// doorbell press fans out to N devices, each with its own independent success/
// failure/retry history, so one record per (event, device) pair keeps that history
// meaningful instead of one field trying to summarize N outcomes at once.
public class Notification {
    private final String notificationId;
    private final String eventId;
    private final String deviceId;
    private NotificationStatus status;
    private int retryCount;

    public Notification(String notificationId, String eventId, String deviceId) {
        this.notificationId = notificationId;
        this.eventId = eventId;
        this.deviceId = deviceId;
        this.status = NotificationStatus.PENDING;
    }

    public synchronized void markSent() {
        status = NotificationStatus.SENT;
    }

    public synchronized void markFailed() {
        status = NotificationStatus.FAILED;
    }

    public synchronized void markDeadLettered() {
        status = NotificationStatus.DEAD_LETTERED;
    }

    public synchronized int incrementAndGetRetryCount() {
        return ++retryCount;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public synchronized NotificationStatus getStatus() {
        return status;
    }

    public synchronized int getRetryCount() {
        return retryCount;
    }
}
