package logsystem;

// LogAgent depends on this INTERFACE, never on a concrete Kafka client - the seam
// the notes imply when they say ingestion "publishes to Kafka" without the agent
// itself needing to know Kafka specifics.
public interface LogCollector {
    void collect(LogEntry log);
}
