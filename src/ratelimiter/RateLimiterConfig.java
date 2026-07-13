package ratelimiter;

import java.time.Duration;

// Builder Pattern from your notes: RateLimiterConfig has three required fields and
// no sensible default for any of them (a rate limiter with an unset maxRequests or
// algorithm is meaningless) - a telescoping constructor
// (RateLimiterConfig(max, window, algo)) would work here too, honestly, since there
// are only three fields. The Builder earns its keep once callers start wanting
// readability at the call site (`.maxRequests(100).window(...).algorithm(...)`
// self-documents which value is which, whereas `new RateLimiterConfig(100, ..., ...)`
// doesn't) - which is exactly the fluent style your notes show.
//
// One config SHAPE describes all four algorithms uniformly (maxRequests + window),
// even though each algorithm interprets "N requests per window" differently
// internally (Fixed/Sliding Window track a hard count; Token/Leaky Bucket derive a
// continuous refill/leak RATE from the same two numbers). Keeping the config uniform
// is what lets RateLimiterFactory build any of the four from the exact same input.
public class RateLimiterConfig {
    private final int maxRequests;
    private final Duration window;
    private final AlgorithmType algorithm;

    private RateLimiterConfig(Builder builder) {
        this.maxRequests = builder.maxRequests;
        this.window = builder.window;
        this.algorithm = builder.algorithm;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public Duration getWindow() {
        return window;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public static class Builder {
        private int maxRequests;
        private Duration window;
        private AlgorithmType algorithm;

        public Builder maxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
            return this;
        }

        public Builder window(Duration window) {
            this.window = window;
            return this;
        }

        public Builder algorithm(AlgorithmType algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public RateLimiterConfig build() {
            if (maxRequests <= 0) {
                throw new IllegalStateException("maxRequests must be positive");
            }
            if (window == null || window.isZero() || window.isNegative()) {
                throw new IllegalStateException("window must be a positive duration");
            }
            if (algorithm == null) {
                throw new IllegalStateException("algorithm must be specified");
            }
            return new RateLimiterConfig(this);
        }
    }
}
