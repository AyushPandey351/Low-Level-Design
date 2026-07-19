package logsystem;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// This is follow-up #1 made real: "payment -> doc1,doc5,doc8; timeout -> doc2,doc5,
// doc10; intersection gives matching documents quickly." For a multi-term
// fullTextQuery, each term's posting list (from LogIndexer's inverted index) is
// retrieved and INTERSECTED (Set.retainAll) - only logIds present in EVERY term's
// list survive, which is exactly what "logs containing both payment AND timeout"
// means. Structured filters (service/level/time range) are applied afterward as a
// final pass, since they're cheap equality/range checks compared to the full-text
// lookup.
public class OpenSearchSearchService implements SearchService {
    private final LogIndexer indexer;

    public OpenSearchSearchService(LogIndexer indexer) {
        this.indexer = indexer;
    }

    @Override
    public List<LogEntry> search(SearchRequest request) {
        List<LogEntry> candidates = resolveCandidates(request);
        return candidates.stream()
                .filter(log -> request.getServiceName() == null || log.getServiceName().equals(request.getServiceName()))
                .filter(log -> request.getLevel() == null || log.getLevel() == request.getLevel())
                .filter(log -> request.getFromTime() == null || !log.getTimestamp().isBefore(request.getFromTime()))
                .filter(log -> request.getToTime() == null || !log.getTimestamp().isAfter(request.getToTime()))
                .collect(Collectors.toList());
    }

    private List<LogEntry> resolveCandidates(SearchRequest request) {
        if (request.getFullTextQuery() != null) {
            Set<String> intersected = null;
            for (String term : OpenSearchIndexer.tokenize(request.getFullTextQuery())) {
                Set<String> postingList = indexer.findLogIdsForTerm(term);
                if (intersected == null) {
                    intersected = new HashSet<>(postingList);
                } else {
                    intersected.retainAll(postingList); // the intersection step
                }
            }
            if (intersected == null) {
                return List.of();
            }
            return intersected.stream().map(indexer::findById).filter(Objects::nonNull).collect(Collectors.toList());
        }
        if (request.getTraceId() != null) {
            return indexer.findByTraceId(request.getTraceId());
        }
        if (request.getServiceName() != null) {
            return indexer.findByService(request.getServiceName());
        }
        return List.of();
    }
}
