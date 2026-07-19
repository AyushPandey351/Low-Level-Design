package ring;

import java.time.Instant;

// The immutable event published by the doorbell, per the notes. eventId is what
// IdempotencyService keys on (redelivery of the SAME id must be a no-op), while
// doorbellId is what the partitioned queue keys on for per-doorbell ordering -
// two different ids serving two different correctness properties, not
// interchangeable even though a naive reading might conflate them.
public class DoorbellEvent {
    private final String eventId;
    private final String homeId;
    private final String doorbellId;
    private final Instant timestamp;

    public DoorbellEvent(String eventId, String homeId, String doorbellId, Instant timestamp) {
        this.eventId = eventId;
        this.homeId = homeId;
        this.doorbellId = doorbellId;
        this.timestamp = timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public String getHomeId() {
        return homeId;
    }

    public String getDoorbellId() {
        return doorbellId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
