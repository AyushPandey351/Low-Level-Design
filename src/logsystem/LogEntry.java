package logsystem;

import java.time.Instant;

// Immutable - one fact an application already logged. traceId is what lets
// follow-up #3 ("correlate logs across microservices") actually work: every
// service touched by the same request stamps its logs with the same traceId, so a
// search for one traceId reconstructs the whole request's path across services.
public class LogEntry {
    private final String logId;
    private final String serviceName;
    private final String host;
    private final LogLevel level;
    private final Instant timestamp;
    private final String traceId;
    private final String message;

    public LogEntry(String logId, String serviceName, String host, LogLevel level,
                     Instant timestamp, String traceId, String message) {
        this.logId = logId;
        this.serviceName = serviceName;
        this.host = host;
        this.level = level;
        this.timestamp = timestamp;
        this.traceId = traceId;
        this.message = message;
    }

    public String getLogId() {
        return logId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getHost() {
        return host;
    }

    public LogLevel getLevel() {
        return level;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + level + "] " + serviceName + "@" + host + " (" + traceId + "): " + message;
    }
}
