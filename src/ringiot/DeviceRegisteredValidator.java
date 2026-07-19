package ringiot;

// The "verify device exists" step - a structurally perfect event referencing a
// deviceId nobody ever registered is still invalid, just for a different reason
// than a malformed schema. Separated from SchemaEventValidator so each validator's
// failure message stays specific to what it actually checked.
public class DeviceRegisteredValidator implements EventValidator {
    private final DeviceRepository deviceRepository;

    public DeviceRegisteredValidator(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public void validate(Event event) {
        if (deviceRepository.find(event.getDeviceId()) == null) {
            throw new InvalidEventException("Unknown device: " + event.getDeviceId());
        }
    }
}
