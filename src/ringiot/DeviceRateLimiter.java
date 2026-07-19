package ringiot;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Concretely answers the follow-up "what if a device sends 1000 events/second
// because of a firmware bug" - a per-device fixed-window counter (same guarded,
// synchronized compound check-reset-increment technique as WindowCounter in the
// Rate Limiter design earlier in this series), plus QUARANTINE: a device that
// exceeds its limit isn't just throttled for one window, it's excluded from
// ingestion entirely until manually cleared - protecting the rest of the pipeline
// from one misbehaving device indefinitely re-testing the limit every window.
public class DeviceRateLimiter {
    private final int maxEventsPerWindow;
    private final long windowSizeMillis;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Set<String> quarantined = ConcurrentHashMap.newKeySet();

    public DeviceRateLimiter(int maxEventsPerWindow, long windowSizeMillis) {
        this.maxEventsPerWindow = maxEventsPerWindow;
        this.windowSizeMillis = windowSizeMillis;
    }

    public boolean isQuarantined(String deviceId) {
        return quarantined.contains(deviceId);
    }

    // Returns false and quarantines the device the moment it exceeds the limit -
    // the caller (DeviceIngestionService) is expected to check isQuarantined()
    // separately up front to reject subsequent events cheaply, without repeating
    // this window-counting logic.
    public boolean allow(String deviceId) {
        Counter counter = counters.computeIfAbsent(deviceId, id -> new Counter());
        boolean withinLimit = counter.increment(maxEventsPerWindow, windowSizeMillis);
        if (!withinLimit) {
            quarantined.add(deviceId);
            System.out.println("[DeviceRateLimiter] Device " + deviceId
                    + " exceeded " + maxEventsPerWindow + " events/window - quarantined");
        }
        return withinLimit;
    }

    private static class Counter {
        private int count;
        private long windowStart = System.currentTimeMillis();

        synchronized boolean increment(int maxEvents, long windowSizeMillis) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowSizeMillis) {
                windowStart = now;
                count = 0;
            }
            if (count < maxEvents) {
                count++;
                return true;
            }
            return false;
        }
    }
}
