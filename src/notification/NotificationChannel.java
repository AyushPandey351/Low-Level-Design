package notification;

// Strategy Pattern, the marquee pattern in this design per your notes. NotificationService
// holds this INTERFACE, never a concrete EmailNotificationChannel directly - adding
// Slack/Teams/In-App tomorrow is a one-class addition, zero changes to
// NotificationService, NotificationFactory's switch aside.
//
// send() throws NotificationDeliveryException on failure rather than returning a
// boolean - this lets RetryService's retry loop use a single try/catch around
// channel.send(...) regardless of which channel it's driving, instead of every
// caller checking a boolean return value and separately reconstructing "why did
// it fail" from nothing.
public interface NotificationChannel {
    void send(Notification notification);
}
