package logsystem;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// "Agent retries -> same log twice -> deduplicate before indexing." Keyed on
// logId (the notes' first option; traceId+timestamp+sequence is the fallback for
// sources that can't generate a stable id). Set.add() on a ConcurrentHashMap-backed
// set is atomic - the same dedup idiom used in every design in this series.
public class LogDeduplicationService {
    private final Set<String> seenLogIds = ConcurrentHashMap.newKeySet();

    public boolean isNew(String logId) {
        return seenLogIds.add(logId);
    }
}
