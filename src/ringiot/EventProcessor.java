package ringiot;

// Strategy Pattern - "a perfect use of the Strategy Pattern," per the notes. The
// Notification Processor consumer group's workers hold this INTERFACE (via
// EventProcessorFactory), never a concrete MotionProcessor directly - adding
// TamperDetectedProcessor tomorrow (the notes' own follow-up example) is one new
// class plus one factory registration, zero changes to any existing processor or
// to the consumer worker that calls them.
public interface EventProcessor {
    void process(Event event);
}
