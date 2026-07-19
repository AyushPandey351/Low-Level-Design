package logsystem;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Follow-up #2's "Older -> Athena/Spark" side of the split - a full, unindexed scan
// over everything in cold storage, deliberately O(n) rather than using any index.
// This is the honest cost of querying data OpenSearchIndexer never saw: there's no
// inverted index, no by-service map, just linear predicate evaluation over every
// archived entry - the same reason real Athena-over-S3 queries are slow-but-cheap
// compared to a warm OpenSearch query, and why LogSearchGateway (next) tries hard to
// avoid routing here unless the request genuinely reaches outside the hot window.
public class ColdStorageQueryService {
    private final StorageService storageService;

    public ColdStorageQueryService(StorageService storageService) {
        this.storageService = storageService;
    }

    public List<LogEntry> scan(Predicate<LogEntry> filter) {
        System.out.println("[ColdStorageQuery] Running a full scan over cold storage (Athena/Spark-style)...");
        return storageService.findAll().stream().filter(filter).collect(Collectors.toList());
    }
}
