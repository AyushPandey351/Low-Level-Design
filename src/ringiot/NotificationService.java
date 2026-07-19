package ringiot;

// "Already designed earlier," per the notes - referring to the separate, complete
// Ring Notification Service design in this same repo. Kept minimal and self-contained
// here rather than importing across packages, since this design's job is to show
// EventProcessor calling OUT to notification logic through an abstraction, not to
// re-implement that entire other system inline.
public interface NotificationService {
    void notify(Event event, String message);
}
