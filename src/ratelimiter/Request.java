package ratelimiter;

// A plain value object representing one incoming request. Worth noting: the actual
// hot-path API (RateLimiter.allowRequest, RateLimitingStrategy.allowRequest) takes a
// raw clientId String directly, matching your notes' own Step 5 API signature -
// Request isn't threaded through that call. It's kept as its own class because it's
// the natural place richer per-request context would live if this design grew (e.g.
// logging every request for an audit trail, or carrying the API endpoint alongside
// the clientId for per-endpoint limits) - introducing it now costs nothing and gives
// that future extension a home without touching the strategies' method signatures.
public class Request {
    private final String clientId;
    private final long timestamp;

    public Request(String clientId, long timestamp) {
        this.clientId = clientId;
        this.timestamp = timestamp;
    }

    public String getClientId() {
        return clientId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
