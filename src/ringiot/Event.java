package ringiot;

import java.time.Instant;
import java.util.Map;

// Immutable - represents a fact a device already reported, same reasoning as every
// event/request value object in this series. `sequenceNumber` is the field worth
// dwelling on: Kafka partition ordering (see KafkaTopic) guarantees that whatever
// arrives for a given device arrives in the order it was PUBLISHED, but it says
// nothing about events that never arrived AT ALL (dropped on a flaky device
// connection, lost before ever reaching the broker). sequenceNumber is what lets
// SequenceGapDetector notice "we got 5 then 8 - where are 6 and 7?" - a
// completely different failure mode from ordering, and one partition ordering alone
// can't detect.
public class Event {
    private final String eventId;
    private final String deviceId;
    private final String homeId;
    private final EventType type;
    private final Instant timestamp;
    private final long sequenceNumber;
    private final Map<String, Object> payload;

    public Event(String eventId, String deviceId, String homeId, EventType type, Instant timestamp,
                 long sequenceNumber, Map<String, Object> payload) {
        this.eventId = eventId;
        this.deviceId = deviceId;
        this.homeId = homeId;
        this.type = type;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getHomeId() {
        return homeId;
    }

    public EventType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return eventId + "[" + type + ", device=" + deviceId + ", seq=" + sequenceNumber + "]";
    }
}
