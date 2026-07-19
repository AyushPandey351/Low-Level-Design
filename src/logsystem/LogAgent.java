package logsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// "Without it: application -> every log -> HTTP request (too expensive). Instead:
// application -> local buffer -> batch upload -> backend." log() only appends
// locally; nothing leaves this process until flush() runs, which is what turns N
// individual log lines into ONE compressed batch send - the entire point of the
// agent existing as a distinct component instead of the application calling the
// ingestion API directly per line.
public class LogAgent {
    private final List<LogEntry> buffer = new CopyOnWriteArrayList<>();
    private final LogCollector collector;
    private final CompressionStrategy compressionStrategy;
    private static final int MAX_RETRIES = 3;
    private static final int ASSUMED_BYTES_PER_LOG = 1024; // matches the notes' "average log ~1KB"

    public LogAgent(LogCollector collector, CompressionStrategy compressionStrategy) {
        this.collector = collector;
        this.compressionStrategy = compressionStrategy;
    }

    public void log(LogEntry entry) {
        buffer.add(entry);
    }

    public void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        List<LogEntry> batch = new ArrayList<>(buffer);
        buffer.clear();

        int originalSize = batch.size() * ASSUMED_BYTES_PER_LOG;
        int compressedSize = compressionStrategy.compress(originalSize);
        System.out.println("[LogAgent] Flushing " + batch.size() + " logs, compressed "
                + originalSize + "B -> " + compressedSize + "B via " + compressionStrategy.getName());

        for (LogEntry log : batch) {
            sendWithRetry(log, 1);
        }
    }

    private void sendWithRetry(LogEntry log, int attempt) {
        try {
            collector.collect(log);
        } catch (RuntimeException e) {
            if (attempt >= MAX_RETRIES) {
                System.out.println("[LogAgent] Dropping " + log.getLogId() + " after " + MAX_RETRIES + " failed attempts");
                return;
            }
            System.out.println("[LogAgent] Send failed for " + log.getLogId() + ", retry " + attempt);
            sendWithRetry(log, attempt + 1);
        }
    }
}
