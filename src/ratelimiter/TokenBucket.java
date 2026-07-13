package ratelimiter;

// Tokens accumulate continuously at `refillRatePerMillis` (derived from
// maxRequests/window), capped at `capacity`, and each allowed request spends
// exactly one. Unlike Fixed/Sliding Window (which track a hard COUNT of past
// requests), Token Bucket tracks available CREDIT - which is what lets it permit a
// burst up to the full capacity right after being idle, then throttle to the steady
// refill rate once exhausted. That bursty-but-bounded behavior is Token Bucket's
// defining trait vs. the two window strategies.
//
// tryConsume() is `synchronized` for the same reason WindowCounter.increment() is:
// refill (read elapsed time, compute new token count) and consume (check >= 1,
// decrement) must happen as ONE atomic step per client, or two concurrent callers
// could both refill-then-check against a stale token count and both succeed when
// only one token was actually available.
public class TokenBucket {
    private double tokens;
    private final double capacity;
    private final double refillRatePerMillis;
    private long lastRefillTime;

    public TokenBucket(double capacity, double refillRatePerMillis) {
        this.capacity = capacity;
        this.tokens = capacity; // start full - a fresh client can burst immediately
        this.refillRatePerMillis = refillRatePerMillis;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;
        if (elapsed > 0) {
            tokens = Math.min(capacity, tokens + elapsed * refillRatePerMillis);
            lastRefillTime = now;
        }
    }
}
