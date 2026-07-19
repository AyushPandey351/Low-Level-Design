package logsystem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// A second, genuinely different StorageService implementation (a smaller
// self-hosted deployment archiving to local disk instead of S3) - proving
// StorageServiceFactory can hand back either one interchangeably, the same "at
// least two real implementations" discipline used for every Strategy interface in
// this series.
public class LocalDiskArchiveService implements StorageService {
    private final Map<String, LogEntry> archived = new ConcurrentHashMap<>();

    @Override
    public void archive(LogEntry log) {
        archived.put(log.getLogId(), log);
        System.out.println("[LocalDisk] archived " + log.getLogId() + " to /var/log/archive/");
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
