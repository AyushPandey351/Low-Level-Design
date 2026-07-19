package ringiot;

// Matches the notes' walkthrough for MOTION exactly: notify, then run AI detection.
// Storing the raw event is deliberately NOT done here - that's the Storage
// Processor consumer group's job (its own independent copy of the stream, see
// KafkaTopic), not something the Notification Processor's strategy needs to
// duplicate.
public class MotionProcessor implements EventProcessor {
    private final NotificationService notificationService;

    public MotionProcessor(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void process(Event event) {
        notificationService.notify(event, "Motion detected on " + event.getDeviceId());
        System.out.println("[AI] Running person/package detection for " + event.getDeviceId());
    }
}
