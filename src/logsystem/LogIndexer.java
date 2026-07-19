package logsystem;

import java.time.Instant;
import java.util.List;
import java.util.Set;

// Strategy Pattern, per the notes ("different indexers"). Broader than the notes'
// literal single index(log) method - the same lesson learned in the Ring Video
// design applies here: SearchService needs to actually QUERY through this
// abstraction (by term, by service, by traceId, by id), and RetentionService needs
// to find and remove aged-out entries through it too. Casting to a concrete
// OpenSearchIndexer to call query methods would be exactly the abstraction leak
// fixed in ObjectStorageService earlier in this repo - so those methods belong on
// the interface, not bolted onto one implementation.
public interface LogIndexer {
    void index(LogEntry log);

    void remove(String logId);

    LogEntry findById(String logId);

    List<LogEntry> findByService(String serviceName);

    List<LogEntry> findByTraceId(String traceId);

    Set<String> findLogIdsForTerm(String term);

    List<LogEntry> findOlderThan(Instant cutoff);
}
