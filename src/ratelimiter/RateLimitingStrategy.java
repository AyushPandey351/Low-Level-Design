package ratelimiter;

// THE most important pattern in this design, per your notes. RateLimiter holds this
// INTERFACE, never a concrete FixedWindowStrategy/TokenBucketStrategy directly - a
// RedisStrategy or AdaptiveStrategy tomorrow is a one-class addition, zero changes
// to RateLimiter.
//
// reset()/remove() are ALSO on this interface, which is worth flagging against your
// notes' own ISP example ("keep only allowRequest()"). In practice they can't be left
// off: RateLimiter.reset(clientId) and RateLimiter.remove(clientId) are real Step 5
// APIs, and the per-client state they need to touch (WindowCounter, the timestamp
// deque, TokenBucket, LeakyBucket) lives INSIDE each concrete strategy's own map -
// RateLimiter has no other way to reach it. The notes' ISP point ("small interfaces
// over one giant one") still holds; it just means this interface's honest minimal
// shape is these three methods, not one.
public interface RateLimitingStrategy {
    boolean allowRequest(String clientId);

    void reset(String clientId);

    void remove(String clientId);
}
