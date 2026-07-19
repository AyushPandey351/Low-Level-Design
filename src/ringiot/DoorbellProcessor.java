package ringiot;

public class DoorbellProcessor implements EventProcessor {
    private final NotificationService notificationService;

    public DoorbellProcessor(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void process(Event event) {
        notificationService.notify(event, "Someone pressed the doorbell (" + event.getDeviceId() + ")");
    }
}
