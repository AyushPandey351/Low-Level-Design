package ringiot;

// The "validate JSON / validate schema" step from the notes' ingestion flow -
// structural checks only (required fields present, sequence number sane). Doesn't
// know or care whether the device actually exists - that's DeviceRegisteredValidator's
// job, composed alongside this one (see CompositeEventValidator), keeping each
// validator's failure reason unambiguous.
public class SchemaEventValidator implements EventValidator {
    @Override
    public void validate(Event event) {
        if (isBlank(event.getEventId())) {
            throw new InvalidEventException("Missing eventId");
        }
        if (isBlank(event.getDeviceId())) {
            throw new InvalidEventException("Missing deviceId");
        }
        if (event.getType() == null) {
            throw new InvalidEventException("Missing eventType");
        }
        if (event.getTimestamp() == null) {
            throw new InvalidEventException("Missing timestamp");
        }
        if (event.getSequenceNumber() < 0) {
            throw new InvalidEventException("Negative sequenceNumber");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
