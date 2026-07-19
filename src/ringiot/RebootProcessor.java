package ringiot;

// A FIFTH processor, beyond the notes' literal four (Motion/Battery/Offline/Doorbell) -
// added specifically to prove follow-up #4 ("how do you process new event types
// without changing existing code") by actually doing it, not just describing it.
// Implementing this interface and registering it in EventProcessorFactory (see
// Main) is the entire change required - MotionProcessor, BatteryProcessor,
// OfflineProcessor, and DoorbellProcessor are untouched.
public class RebootProcessor implements EventProcessor {
    private final DeviceRepository deviceRepository;

    public RebootProcessor(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public void process(Event event) {
        Device device = deviceRepository.find(event.getDeviceId());
        device.setStatus(DeviceStatus.ONLINE);
        device.touch();
        System.out.println("[RebootProcessor] " + event.getDeviceId() + " rebooted and is back online");
    }
}
