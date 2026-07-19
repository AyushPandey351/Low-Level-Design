package ring;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Concretely answers the Amazon follow-up "how do you prevent notification storms
// if someone repeatedly presses the doorbell" - not listed among the notes' core
// Part 3 classes, but built anyway since the follow-up explicitly asks for it and a
// comment alone wouldn't actually demonstrate it.
//
// Deliberately a SEPARATE class from IdempotencyService, even though both look like
// "have we seen this key recently" checks - they answer different questions.
// IdempotencyService asks "have we already processed this EXACT event id" (a
// redelivery of the SAME press must never double-notify). Debouncer asks "was there
// a DIFFERENT, legitimate press for this SAME doorbell too recently" (a person
// mashing the button five times in two seconds should trigger ONE notification
// burst, not five) - a real new event, deliberately suppressed anyway. Conflating
// them into one class would make it unclear which guarantee is actually being
// enforced.
public class Debouncer {
    private final Duration window;
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public Debouncer(Duration window) {
        this.window = window;
    }

    // Atomic per-key check via compute(): returns true if `key` was seen within the
    // debounce window (caller should suppress), false if this call is far enough
    // past the last one to count as a fresh trigger (and this call now becomes the
    // new "last seen" timestamp for the next check).
    public boolean shouldSuppress(String key) {
        Instant now = Instant.now();
        boolean[] suppress = {false};
        lastSeen.compute(key, (k, last) -> {
            if (last != null && Duration.between(last, now).compareTo(window) < 0) {
                suppress[0] = true;
                return last; // keep the original timestamp - this press doesn't count
            }
            return now;
        });
        return suppress[0];
    }
}
