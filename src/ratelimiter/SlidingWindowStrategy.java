package ratelimiter;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Fixes Fixed Window's classic edge-case bug (worth knowing even though it's not
// explicitly in your notes): a client could send maxRequests at 0:59 and ANOTHER
// full maxRequests at 1:01, doubling the effective rate right at the window
// boundary, because Fixed Window resets to zero on the clock rather than tracking a
// rolling window. Sliding Window fixes this by evicting only timestamps OLDER than
// `windowSizeMillis` before each check - the limit applies to any rolling
// windowSizeMillis-wide slice of time, not to clock-aligned buckets.
//
// synchronized(deque) locks on the per-client Deque object itself for the same
// per-client-mutual-exclusion reason WindowCounter uses `synchronized` on itself -
// ArrayDeque isn't thread-safe, and the evict-check-add sequence must run as one
// atomic unit per client (two threads evicting and checking size concurrently could
// otherwise both see room for one more and both add, exceeding the limit).
public class SlidingWindowStrategy implements RateLimitingStrategy {
    private final int maxRequests;
    private final long windowSizeMillis;
    private final Map<String, Deque<Long>> timestamps = new ConcurrentHashMap<>();

    public SlidingWindowStrategy(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = window.toMillis();
    }

    @Override
    public boolean allowRequest(String clientId) {
        Deque<Long> deque = timestamps.computeIfAbsent(clientId, id -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() >= windowSizeMillis) {
                deque.pollFirst();
            }
            if (deque.size() < maxRequests) {
                deque.addLast(now);
                return true;
            }
            return false;
        }
    }

    @Override
    public void reset(String clientId) {
        Deque<Long> deque = timestamps.get(clientId);
        if (deque != null) {
            synchronized (deque) {
                deque.clear();
            }
        }
    }

    @Override
    public void remove(String clientId) {
        timestamps.remove(clientId);
    }
}
