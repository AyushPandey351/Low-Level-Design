package ringiot;

import java.util.EnumMap;
import java.util.Map;

// Factory Pattern from the notes - "returns the appropriate EventProcessor based on
// the event type." Backed by a Map with a registration hook (rather than a bare
// switch) so RebootProcessor - and any future TamperDetectedProcessor - can be
// wired in from outside this class without editing it.
public class EventProcessorFactory {
    private final Map<EventType, EventProcessor> processors = new EnumMap<>(EventType.class);

    public void register(EventType type, EventProcessor processor) {
        processors.put(type, processor);
    }

    public EventProcessor getProcessor(EventType type) {
        EventProcessor processor = processors.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("No processor registered for event type: " + type);
        }
        return processor;
    }
}
