package logsystem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// "Cold storage - older logs - S3 - cheap and durable." Append-heavy, rarely
// point-queried by id (the access pattern is bulk scans via Athena/Spark, modeled
// by ColdStorageQueryService), matching the same S3 access-pattern reasoning used
// in the Ring Video and Ring IoT designs.
public class S3ArchiveService implements StorageService {
    private final Map<String, LogEntry> archived = new ConcurrentHashMap<>();

    @Override
    public void archive(LogEntry log) {
        archived.put(log.getLogId(), log);
    }

    @Override
    public void delete(String logId) {
        archived.remove(logId);
    }

    @Override
    public List<LogEntry> findAll() {
        return List.copyOf(archived.values());
    }
}
