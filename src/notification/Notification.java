package notification;

import java.time.LocalDateTime;

// Represents ONE delivery attempt through ONE channel - worth flagging as a
// deliberate reading of "one notification can be sent through multiple channels."
// The notes give Notification a single `status` field, not a per-channel map of
// statuses. Rather than inventing an unlisted PARTIAL status (or bolting a
// Map<ChannelType, NotificationStatus> onto this class to track several channels'
// outcomes at once), NotificationService fans a multi-channel send request out into
// one Notification PER requested channel - each gets its own id, its own status,
// its own retry history. "One logical message, multiple channels" becomes "one
// logical message, multiple Notification records" - which keeps this class's single
// status field meaningful (it always describes exactly one channel's delivery
// outcome) instead of overloading it to summarize several independent outcomes at once.
public class Notification {
    private final String notificationId;
    private final String title;
    private final String message;
    private final User recipient;
    private final ChannelType channel;
    private final LocalDateTime createdAt;
    private NotificationStatus status;
    private int retryCount;

    public Notification(String notificationId, String title, String message, User recipient, ChannelType channel) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.recipient = recipient;
        this.channel = channel;
        this.createdAt = LocalDateTime.now();
        this.status = NotificationStatus.PENDING;
    }

    public void markSent() {
        status = NotificationStatus.SENT;
    }

    public void markFailed() {
        status = NotificationStatus.FAILED;
    }

    public void markRetrying() {
        status = NotificationStatus.RETRYING;
        retryCount++;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public User getRecipient() {
        return recipient;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
