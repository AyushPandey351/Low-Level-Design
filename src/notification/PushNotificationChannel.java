package notification;

public class PushNotificationChannel implements NotificationChannel {
    @Override
    public void send(Notification notification) {
        System.out.println("[Push -> device " + notification.getRecipient().getDeviceToken() + "] "
                + notification.getTitle());
    }
}
