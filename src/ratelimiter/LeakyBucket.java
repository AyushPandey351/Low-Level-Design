package ratelimiter;

// The mathematical MIRROR of TokenBucket, worth reading side by side: Token Bucket
// tracks CREDIT that accumulates over time and is spent by each request. Leaky
// Bucket tracks LOAD (`currentLevel`) that's ADDED BY each request and drains
// ("leaks") away at a constant rate over time. Same two numbers (capacity, rate
// derived from maxRequests/window), same refill-then-check-then-mutate shape,
// opposite direction. This is why LeakyBucketStrategy is the one place in this
// design that DOESN'T get its own helper-state-management comment beyond this one -
// once you see TokenBucket, LeakyBucket is "that, inverted."
//
// Practical consequence of the inversion: Token Bucket lets a burst through
// immediately (starts full of credit) then throttles; Leaky Bucket starts EMPTY
// (currentLevel = 0) and enforces a smooth, constant processing rate from the very
// first request - no initial burst allowance. That's the real behavioral distinction
// an interviewer is checking for when they ask "Token Bucket vs Leaky Bucket."
public class LeakyBucket {
    private double currentLevel;
    private final double capacity;
    private final double leakRatePerMillis;
    private long lastLeakTime;

    public LeakyBucket(double capacity, double leakRatePerMillis) {
        this.capacity = capacity;
        this.leakRatePerMillis = leakRatePerMillis;
        this.lastLeakTime = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        leak();
        if (currentLevel + 1 <= capacity) {
            currentLevel += 1;
            return true;
        }
        return false;
    }

    private void leak() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastLeakTime;
        if (elapsed > 0) {
            currentLevel = Math.max(0, currentLevel - elapsed * leakRatePerMillis);
            lastLeakTime = now;
        }
    }
}
