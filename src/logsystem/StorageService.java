package logsystem;

import java.util.List;

// Strategy Pattern - "different storage backends (S3, GCS, Azure Blob)." Broader
// than a single archive() method for the same reason as LogIndexer: RetentionService
// needs to delete expired cold logs, and ColdStorageQueryService needs to scan
// everything, through this abstraction rather than a concrete cast.
public interface StorageService {
    void archive(LogEntry log);

    void delete(String logId);

    List<LogEntry> findAll();
}
