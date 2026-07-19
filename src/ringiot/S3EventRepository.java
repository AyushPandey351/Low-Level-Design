package ringiot;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Stands in for "cold data - historical events - stored in S3, later queried with
// Athena/Spark/EMR." Append-only by nature (S3 objects aren't mutated in place),
// which is exactly what this does - events accumulate, nothing is ever updated or
// removed, matching the notes' own note that this is "usually stored in S3 rather
// than a relational DB due to volume" (an append-heavy, rarely-queried-by-primary-key
// access pattern, unlike DeviceRepository's point lookups).
public class S3EventRepository implements EventRepository {
    private final List<Event> events = new CopyOnWriteArrayList<>();

    @Override
    public void save(Event event) {
        events.add(event);
    }

    public List<Event> getAll() {
        return events;
    }
}
