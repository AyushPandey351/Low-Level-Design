package ratelimiter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LeakyBucketStrategy implements RateLimitingStrategy {
    private final double capacity;
    private final double leakRatePerMillis;
    private final Map<String, LeakyBucket> buckets = new ConcurrentHashMap<>();

    public LeakyBucketStrategy(int maxRequests, Duration window) {
        this.capacity = maxRequests;
        this.leakRatePerMillis = (double) maxRequests / window.toMillis();
    }

    @Override
    public boolean allowRequest(String clientId) {
        LeakyBucket bucket = buckets.computeIfAbsent(clientId, id -> new LeakyBucket(capacity, leakRatePerMillis));
        return bucket.allowRequest();
    }

    @Override
    public void reset(String clientId) {
        buckets.put(clientId, new LeakyBucket(capacity, leakRatePerMillis));
    }

    @Override
    public void remove(String clientId) {
        buckets.remove(clientId);
    }
}
