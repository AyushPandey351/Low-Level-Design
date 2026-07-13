package ratelimiter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucketStrategy implements RateLimitingStrategy {
    private final double capacity;
    private final double refillRatePerMillis;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketStrategy(int maxRequests, Duration window) {
        this.capacity = maxRequests;
        this.refillRatePerMillis = (double) maxRequests / window.toMillis();
    }

    @Override
    public boolean allowRequest(String clientId) {
        TokenBucket bucket = buckets.computeIfAbsent(clientId, id -> new TokenBucket(capacity, refillRatePerMillis));
        return bucket.tryConsume();
    }

    @Override
    public void reset(String clientId) {
        buckets.put(clientId, new TokenBucket(capacity, refillRatePerMillis));
    }

    @Override
    public void remove(String clientId) {
        buckets.remove(clientId);
    }
}
