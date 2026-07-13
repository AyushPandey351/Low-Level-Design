package notification;

public class SMSNotificationChannel implements NotificationChannel {
    @Override
    public void send(Notification notification) {
        System.out.println("[SMS -> " + notification.getRecipient().getPhone() + "] "
                + notification.getMessage());
    }
}
