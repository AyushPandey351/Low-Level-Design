package notification;

// The "future extension" from your notes, built now to prove the point: adding it
// required exactly one new file, no changes anywhere else - NotificationFactory's
// switch statement is the only other place that even needs to know it exists.
public class WhatsAppNotificationChannel implements NotificationChannel {
    @Override
    public void send(Notification notification) {
        System.out.println("[WhatsApp -> " + notification.getRecipient().getPhone() + "] "
                + notification.getMessage());
    }
}
