package ring;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// The core orchestrator, matching the notes' process() skeleton closely, with two
// deliberate departures worth calling out:
//
// 1. Debounce runs BEFORE idempotency, not instead of it. These check different
//    things (see Debouncer's comment) and both matter: debounce catches "five real
//    presses in two seconds," idempotency catches "the same one press redelivered
//    by an at-least-once queue." Checking debounce first is also cheaper - no point
//    doing a Redis idempotency lookup for an event we're about to suppress anyway.
//
// 2. Uses idempotencyService.tryMarkProcessed() (one atomic call), not the notes'
//    literal alreadyProcessed()-then-markProcessed() pair - see IdempotencyService's
//    comment for exactly why that two-step version has a real race under concurrent
//    workers.
public class NotificationService {
    private final DeviceRepository deviceRepository;
    private final PushProviderFactory providerFactory;
    private final NotificationRepository notificationRepository;
    private final RetryService retryService;
    private final IdempotencyService idempotencyService;
    private final Debouncer debouncer;
    private final AtomicInteger idCounter = new AtomicInteger();

    public NotificationService(DeviceRepository deviceRepository, PushProviderFactory providerFactory,
                                NotificationRepository notificationRepository, RetryService retryService,
                                IdempotencyService idempotencyService, Debouncer debouncer) {
        this.deviceRepository = deviceRepository;
        this.providerFactory = providerFactory;
        this.notificationRepository = notificationRepository;
        this.retryService = retryService;
        this.idempotencyService = idempotencyService;
        this.debouncer = debouncer;
    }

    public void process(DoorbellEvent event) {
        if (debouncer.shouldSuppress(event.getDoorbellId())) {
            System.out.println("[NotificationService] Debounced " + event.getEventId()
                    + " - too soon after the last press on doorbell " + event.getDoorbellId());
            return;
        }

        if (!idempotencyService.tryMarkProcessed(event.getEventId())) {
            System.out.println("[NotificationService] Skipping duplicate delivery of event "
                    + event.getEventId() + " (already processed)");
            return;
        }

        List<Device> devices = deviceRepository.findByHome(event.getHomeId());
        for (Device device : devices) {
            Notification notification = new Notification(
                    "NOTIF" + idCounter.incrementAndGet(), event.getEventId(), device.getDeviceId());
            notificationRepository.save(notification);

            PushProvider provider = providerFactory.getProvider(device.getPlatform());
            try {
                provider.send(notification, device);
                notification.markSent();
                notificationRepository.update(notification);
            } catch (PushDeliveryException e) {
                notification.markFailed();
                notificationRepository.update(notification);
                retryService.scheduleRetry(notification, device, provider);
            }
        }
    }
}
