package ringiot;

// Thrown by any EventValidator - DeviceIngestionService catches this specifically
// and routes the offending event to the DLQ rather than letting a malformed event
// anywhere near Kafka. "Never block the entire pipeline" over one bad event.
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}
