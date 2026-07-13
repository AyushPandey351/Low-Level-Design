package ratelimiter;

// This is where Step 8's race condition actually gets fixed - and it's worth being
// precise about WHY a plain AtomicInteger alone (Solution 2 in your notes) isn't
// quite enough here. The compound operation is "if the window has expired, reset
// BOTH requestCount and windowStartTime together, THEN check-and-increment" - that's
// two related fields that must change as one atomic unit. An AtomicInteger only
// makes a SINGLE int atomic; it can't also atomically coordinate resetting
// windowStartTime in the same step. So increment() is a `synchronized` method
// instead - the whole check-reset-check-increment sequence runs as one atomic block
// per client, which is what actually prevents two threads from both reading
// count=99, both incrementing, and landing on 101.
//
// This also answers the notes' "Solution 1: per-client lock, using
// ConcurrentHashMap<String, ReentrantLock>" - a SEPARATE lock map is unnecessary
// here: locking on the per-client WindowCounter object itself (via `synchronized`)
// achieves identical per-client mutual exclusion, without a second map that could
// drift out of sync with the data map (e.g. forgetting to also evict a client's lock
// when remove() evicts their counter).
public class WindowCounter {
    private int requestCount;
    private long windowStartTime;

    public WindowCounter() {
        this.windowStartTime = System.currentTimeMillis();
    }

    public synchronized boolean increment(int maxRequests, long windowSizeMillis) {
        long now = System.currentTimeMillis();
        if (now - windowStartTime >= windowSizeMillis) {
            windowStartTime = now;
            requestCount = 0;
        }
        if (requestCount < maxRequests) {
            requestCount++;
            return true;
        }
        return false;
    }

    public synchronized void reset() {
        requestCount = 0;
        windowStartTime = System.currentTimeMillis();
    }
}
