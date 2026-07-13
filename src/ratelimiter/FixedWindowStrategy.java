package ratelimiter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// ConcurrentHashMap (Solution 3 from your notes) for the client -> counter map itself:
// a plain HashMap under concurrent computeIfAbsent() calls from different clients
// hitting the limiter at once risks corrupting the map's internal structure, not
// just losing an update. That's a DIFFERENT concern from WindowCounter's own
// synchronized increment() - this map protects the MAP, the synchronized method
// protects each individual CLIENT'S counter fields.
public class FixedWindowStrategy implements RateLimitingStrategy {
    private final int maxRequests;
    private final long windowSizeMillis;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public FixedWindowStrategy(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = window.toMillis();
    }

    @Override
    public boolean allowRequest(String clientId) {
        WindowCounter counter = counters.computeIfAbsent(clientId, id -> new WindowCounter());
        return counter.increment(maxRequests, windowSizeMillis);
    }

    @Override
    public void reset(String clientId) {
        WindowCounter counter = counters.get(clientId);
        if (counter != null) {
            counter.reset();
        }
    }

    @Override
    public void remove(String clientId) {
        counters.remove(clientId);
    }
}
