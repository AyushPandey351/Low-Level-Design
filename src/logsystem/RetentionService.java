package logsystem;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// "Hot 7 Days -> Move -> S3 -> Delete 90 Days." Worth being precise about what
// "move" means given this design's own architecture: the Archive Worker (its own
// Kafka consumer group, see Main) already writes a durable copy of every log to
// cold storage CONTINUOUSLY, as it arrives - not just after 7 days. So by the time
// a log ages out of the hot window, it's already safely archived. hotToColdSweep()
// therefore only needs to REMOVE the entry from the hot index (freeing expensive,
// finite OpenSearch storage) - calling storageService.archive() again here would
// just be redundant duplicate writes to something already durably stored.
// coldDeletionSweep() is the separate, later stage: permanently deleting anything
// past the FULL retention window, regardless of tier.
public class RetentionService {
    private final LogIndexer indexer;
    private final StorageService storageService;
    private final Duration hotRetention;
    private final Duration totalRetention;

    public RetentionService(LogIndexer indexer, StorageService storageService, Duration hotRetention, Duration totalRetention) {
        this.indexer = indexer;
        this.storageService = storageService;
        this.hotRetention = hotRetention;
        this.totalRetention = totalRetention;
    }

    public void hotToColdSweep() {
        Instant hotCutoff = Instant.now().minus(hotRetention);
        List<LogEntry> aged = indexer.findOlderThan(hotCutoff);
        for (LogEntry log : aged) {
            indexer.remove(log.getLogId());
            System.out.println("[Retention] Evicted " + log.getLogId() + " from hot storage (already durably archived)");
        }
    }

    public void coldDeletionSweep() {
        Instant totalCutoff = Instant.now().minus(totalRetention);
        for (LogEntry log : storageService.findAll()) {
            if (log.getTimestamp().isBefore(totalCutoff)) {
                storageService.delete(log.getLogId());
                System.out.println("[Retention] Permanently deleted " + log.getLogId()
                        + " after " + totalRetention.toDays() + "-day total retention");
            }
        }
    }
}
