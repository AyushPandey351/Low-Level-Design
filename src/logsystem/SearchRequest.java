package logsystem;

import java.time.Instant;

// Every field is optional, which is exactly the multi-optional-parameter situation
// the Builder pattern fits (same reasoning as RateLimiterConfig earlier in this
// repo) - a raw constructor would force callers to pass null for every filter they
// don't care about, in a fixed positional order that's easy to get wrong.
public class SearchRequest {
    private final String serviceName;
    private final LogLevel level;
    private final String traceId;
    private final Instant fromTime;
    private final Instant toTime;
    private final String fullTextQuery;

    private SearchRequest(Builder builder) {
        this.serviceName = builder.serviceName;
        this.level = builder.level;
        this.traceId = builder.traceId;
        this.fromTime = builder.fromTime;
        this.toTime = builder.toTime;
        this.fullTextQuery = builder.fullTextQuery;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getServiceName() {
        return serviceName;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getFromTime() {
        return fromTime;
    }

    public Instant getToTime() {
        return toTime;
    }

    public String getFullTextQuery() {
        return fullTextQuery;
    }

    public static class Builder {
        private String serviceName;
        private LogLevel level;
        private String traceId;
        private Instant fromTime;
        private Instant toTime;
        private String fullTextQuery;

        public Builder service(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder from(Instant fromTime) {
            this.fromTime = fromTime;
            return this;
        }

        public Builder to(Instant toTime) {
            this.toTime = toTime;
            return this;
        }

        public Builder fullText(String query) {
            this.fullTextQuery = query;
            return this;
        }

        public SearchRequest build() {
            return new SearchRequest(this);
        }
    }
}
