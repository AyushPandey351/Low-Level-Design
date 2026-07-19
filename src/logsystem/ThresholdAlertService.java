package logsystem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// "ERROR > 100/min -> Slack/PagerDuty/Email. No need to query OpenSearch
// continuously" - this consumes the SAME Kafka stream directly (its own consumer
// group), evaluating in-stream rather than periodically polling the search index,
// which is exactly what "no need to query continuously" means in practice.
//
// Per-service fixed-window ERROR/FATAL counter, same synchronized compound
// check-reset-increment technique as WindowCounter (Rate Limiter design) and
// DeviceRateLimiter (Ring IoT design) - and the SAME reason: the window-reset and
// the increment must happen as one atomic step per service, or two concurrent log
// arrivals could both read a stale count and under-count real error bursts.
public class ThresholdAlertService implements AlertService {
    private final int errorThreshold;
    private final long windowMillis;
    private final Map<String, Counter> countersByService = new ConcurrentHashMap<>();

    public ThresholdAlertService(int errorThreshold, long windowMillis) {
        this.errorThreshold = errorThreshold;
        this.windowMillis = windowMillis;
    }

    @Override
    public void evaluate(LogEntry log) {
        if (log.getLevel() != LogLevel.ERROR && log.getLevel() != LogLevel.FATAL) {
            return;
        }
        Counter counter = countersByService.computeIfAbsent(log.getServiceName(), k -> new Counter());
        if (counter.incrementAndCheck(errorThreshold, windowMillis)) {
            System.out.println("[AlertService] " + log.getServiceName() + " exceeded " + errorThreshold
                    + " ERROR/FATAL logs in the current window -> notifying Slack/PagerDuty/Email");
        }
    }

    private static class Counter {
        private int count;
        private long windowStart = System.currentTimeMillis();

        // Fires (returns true) exactly once per window, the instant the threshold
        // is CROSSED - not on every subsequent log after the breach, which would
        // otherwise spam a notification per log for the rest of the window.
        synchronized boolean incrementAndCheck(int threshold, long windowMillis) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                count = 0;
            }
            count++;
            return count == threshold;
        }
    }
}
