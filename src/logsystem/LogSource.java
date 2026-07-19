package logsystem;

import java.time.Duration;
import java.time.Instant;

// The optional metadata DB from the notes - "main logs themselves are not stored
// in an RDBMS," only this small per-service configuration record is.
public class LogSource {
    private final String serviceId;
    private final String owner;
    private final Duration retentionPolicy;
    private final Instant createdAt;

    public LogSource(String serviceId, String owner, Duration retentionPolicy, Instant createdAt) {
        this.serviceId = serviceId;
        this.owner = owner;
        this.retentionPolicy = retentionPolicy;
        this.createdAt = createdAt;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getOwner() {
        return owner;
    }

    public Duration getRetentionPolicy() {
        return retentionPolicy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
