package ratelimiter;

// Centralizes "given a config, build the right strategy" - one switch here instead
// of RateLimiter (or whoever else needs a strategy) branching on algorithm type
// itself. Adding RedisStrategy/AdaptiveStrategy later means one new branch here plus
// the new class - RateLimiter never changes.
public class RateLimiterFactory {
    public static RateLimitingStrategy create(RateLimiterConfig config) {
        switch (config.getAlgorithm()) {
            case FIXED_WINDOW:
                return new FixedWindowStrategy(config.getMaxRequests(), config.getWindow());
            case SLIDING_WINDOW:
                return new SlidingWindowStrategy(config.getMaxRequests(), config.getWindow());
            case TOKEN_BUCKET:
                return new TokenBucketStrategy(config.getMaxRequests(), config.getWindow());
            case LEAKY_BUCKET:
                return new LeakyBucketStrategy(config.getMaxRequests(), config.getWindow());
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + config.getAlgorithm());
        }
    }
}
