package ring;

public interface NotificationRepository {
    void save(Notification notification);

    void update(Notification notification);

    Notification find(String notificationId);
}
