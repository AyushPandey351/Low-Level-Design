package ringiot;

public class SimpleNotificationService implements NotificationService {
    @Override
    public void notify(Event event, String message) {
        System.out.println("[Notify] home " + event.getHomeId() + ": " + message);
    }
}
