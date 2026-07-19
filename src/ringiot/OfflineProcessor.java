package ringiot;

// Handles both CAMERA_OFFLINE and WIFI_DISCONNECTED (registered twice in
// EventProcessorFactory) - both are, from the processing side, the same "mark
// offline, alert, record a metric" action, matching the notes' "For Offline: Mark
// Device Offline -> Alert User -> Metrics."
public class OfflineProcessor implements EventProcessor {
    private final DeviceRepository deviceRepository;
    private final NotificationService notificationService;

    public OfflineProcessor(DeviceRepository deviceRepository, NotificationService notificationService) {
        this.deviceRepository = deviceRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void process(Event event) {
        Device device = deviceRepository.find(event.getDeviceId());
        device.setStatus(DeviceStatus.OFFLINE);
        notificationService.notify(event, event.getDeviceId() + " is offline");
        System.out.println("[Metrics] offline_event device=" + event.getDeviceId() + " type=" + event.getType());
    }
}
