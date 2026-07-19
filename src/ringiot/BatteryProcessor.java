package ringiot;

public class BatteryProcessor implements EventProcessor {
    private final DeviceRepository deviceRepository;
    private final NotificationService notificationService;

    public BatteryProcessor(DeviceRepository deviceRepository, NotificationService notificationService) {
        this.deviceRepository = deviceRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void process(Event event) {
        Device device = deviceRepository.find(event.getDeviceId());
        Object level = event.getPayload().get("batteryLevel");
        if (level instanceof Number) {
            device.setBatteryLevel(((Number) level).intValue());
        }
        notificationService.notify(event, "Battery low on " + event.getDeviceId());
    }
}
