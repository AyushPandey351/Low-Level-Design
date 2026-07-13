package notification;

// Simulated - per the assumption "ignore actual third-party providers (SMTP, Twilio,
// FCM)," this stands in for a real SMTP integration, same simulated-boundary
// approach as GatewayConnector (Payment Gateway) and CardReader (ATM Machine).
public class EmailNotificationChannel implements NotificationChannel {
    @Override
    public void send(Notification notification) {
        System.out.println("[Email -> " + notification.getRecipient().getEmail() + "] "
                + notification.getTitle() + ": " + notification.getMessage());
    }
}
