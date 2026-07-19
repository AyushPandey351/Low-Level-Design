package ringiot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// A DIFFERENT concern from Kafka partition ordering, worth being precise about:
// partitioning by deviceId guarantees that whatever arrives for a device arrives in
// the order it was PUBLISHED - it says nothing about events that were dropped
// before ever reaching the broker (a flaky device connection losing an event
// entirely). This class is purely a DETECTOR, not a fixer - it logs an anomaly when
// a device's sequenceNumber jumps by more than 1, which is the only way to notice
// "events 6 and 7 are just... missing" since a dropped event obviously never
// arrives to be ordered in the first place.
public class SequenceGapDetector {
    private final Map<String, Long> lastSequenceByDevice = new ConcurrentHashMap<>();

    public void check(Event event) {
        Long previous = lastSequenceByDevice.get(event.getDeviceId());
        if (previous != null && event.getSequenceNumber() > previous + 1) {
            long missingCount = event.getSequenceNumber() - previous - 1;
            System.out.println("[SequenceGapDetector] Device " + event.getDeviceId() + ": gap detected - "
                    + missingCount + " event(s) missing between sequence " + previous + " and " + event.getSequenceNumber());
        }
        lastSequenceByDevice.merge(event.getDeviceId(), event.getSequenceNumber(), Math::max);
    }
}
