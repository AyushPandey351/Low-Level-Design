package ratelimiter;

// Singleton ("one shared instance across application," per your notes) via the same
// initialize()/getInstance() split used for ParkingLot/PaymentProcessor/SeatLockService
// in earlier designs - this needs a configured RateLimiterConfig at creation time.
//
// `strategy` is `volatile` for a specific, easy-to-miss reason: updateConfig() can
// run on one thread (e.g. an admin action) while allowRequest() is being called
// concurrently on many others. Without `volatile`, the Java Memory Model doesn't
// guarantee those other threads ever OBSERVE the new strategy reference - they could
// keep reading a stale, cached copy of the old one indefinitely. `volatile` forces
// every read to see the latest write, so a config update takes effect for all
// callers immediately, not "eventually, maybe."
//
// Every public method is a one-line delegation to `strategy` - RateLimiter itself
// contains no algorithm-specific logic at all, which is the DIP callout from your
// notes made concrete: it depends on the RateLimitingStrategy interface, never on
// SlidingWindowStrategy or any other concrete implementation.
public class RateLimiter {
    private static RateLimiter instance;

    private volatile RateLimitingStrategy strategy;

    private RateLimiter(RateLimiterConfig config) {
        this.strategy = RateLimiterFactory.create(config);
    }

    public static synchronized RateLimiter initialize(RateLimiterConfig config) {
        if (instance != null) {
            throw new IllegalStateException("RateLimiter is already initialized");
        }
        instance = new RateLimiter(config);
        return instance;
    }

    public static synchronized RateLimiter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RateLimiter has not been initialized");
        }
        return instance;
    }

    public boolean allowRequest(String clientId) {
        return strategy.allowRequest(clientId);
    }

    public void updateConfig(RateLimiterConfig newConfig) {
        this.strategy = RateLimiterFactory.create(newConfig);
    }

    public void reset(String clientId) {
        strategy.reset(clientId);
    }

    public void remove(String clientId) {
        strategy.remove(clientId);
    }
}
