package logsystem;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

// "Fields indexed: timestamp, service, host, traceId, level. Message text uses a
// full-text index." Structured fields get their own direct map (O(1) lookup by
// service/traceId/id - what a real OpenSearch "keyword" field mapping gives you);
// `message` gets a genuine inverted index (term -> set of logIds), which is the
// data structure that makes follow-up #1's "payment -> doc1,doc5; timeout ->
// doc2,doc5; intersect -> doc5" example actually computable (see
// OpenSearchSearchService, which does the intersection).
public class OpenSearchIndexer implements LogIndexer {
    private final Map<String, LogEntry> byLogId = new ConcurrentHashMap<>();
    private final Map<String, List<LogEntry>> byService = new ConcurrentHashMap<>();
    private final Map<String, List<LogEntry>> byTraceId = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();

    @Override
    public void index(LogEntry log) {
        byLogId.put(log.getLogId(), log);
        byService.computeIfAbsent(log.getServiceName(), k -> new CopyOnWriteArrayList<>()).add(log);
        byTraceId.computeIfAbsent(log.getTraceId(), k -> new CopyOnWriteArrayList<>()).add(log);
        for (String term : tokenize(log.getMessage())) {
            invertedIndex.computeIfAbsent(term, k -> ConcurrentHashMap.newKeySet()).add(log.getLogId());
        }
    }

    @Override
    public void remove(String logId) {
        LogEntry log = byLogId.remove(logId);
        if (log == null) {
            return;
        }
        List<LogEntry> serviceList = byService.get(log.getServiceName());
        if (serviceList != null) {
            serviceList.remove(log);
        }
        List<LogEntry> traceList = byTraceId.get(log.getTraceId());
        if (traceList != null) {
            traceList.remove(log);
        }
        for (String term : tokenize(log.getMessage())) {
            Set<String> ids = invertedIndex.get(term);
            if (ids != null) {
                ids.remove(logId);
            }
        }
    }

    @Override
    public LogEntry findById(String logId) {
        return byLogId.get(logId);
    }

    @Override
    public List<LogEntry> findByService(String serviceName) {
        return byService.getOrDefault(serviceName, List.of());
    }

    @Override
    public List<LogEntry> findByTraceId(String traceId) {
        return byTraceId.getOrDefault(traceId, List.of());
    }

    @Override
    public Set<String> findLogIdsForTerm(String term) {
        return invertedIndex.getOrDefault(term.toLowerCase(), Set.of());
    }

    @Override
    public List<LogEntry> findOlderThan(Instant cutoff) {
        return byLogId.values().stream()
                .filter(log -> log.getTimestamp().isBefore(cutoff))
                .collect(Collectors.toList());
    }

    static List<String> tokenize(String message) {
        return java.util.Arrays.stream(message.toLowerCase().split("\\W+"))
                .filter(term -> !term.isBlank())
                .collect(Collectors.toList());
    }
}
