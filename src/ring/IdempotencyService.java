package ring;

// Backed by Redis in production, per the notes. `tryMarkProcessed` is NOT part of
// the notes' literal two-method interface - it's added because the literal
// interface (`alreadyProcessed()` then, separately, `markProcessed()`) is a
// textbook check-then-act race: two workers processing the SAME redelivered eventId
// concurrently could both call alreadyProcessed(), both see false, and both proceed
// to send duplicate notifications before either calls markProcessed() - exactly the
// "Ding! Ding!" bug the notes are trying to prevent, just reintroduced by the
// two-step API shape. tryMarkProcessed() is the atomic, race-free version
// NotificationService actually uses; the two separate methods stay for read-only
// status checks (e.g. admin tooling asking "has this been handled") where atomicity
// doesn't matter.
public interface IdempotencyService {
    boolean alreadyProcessed(String eventId);

    void markProcessed(String eventId);

    // Atomically checks-and-marks in one step. Returns true only for the FIRST
    // caller to claim this eventId (or the first caller after its TTL expired);
    // every other concurrent or subsequent caller gets false.
    boolean tryMarkProcessed(String eventId);
}
