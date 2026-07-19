package ringiot;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// "Malformed JSON -> cannot parse -> DLQ. Never block the entire pipeline." A
// single bad event from one device must never stall ingestion for every other
// device - this is where it goes instead, inspectable later rather than silently dropped.
public class DeadLetterQueue {
    private final List<String> entries = new CopyOnWriteArrayList<>();

    public void add(Event event, String reason) {
        entries.add(event.getEventId() + " (device " + event.getDeviceId() + "): " + reason);
    }

    public List<String> getAll() {
        return entries;
    }
}
