package logsystem;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Follow-up #2, made real: "Last 7 days -> OpenSearch. Older -> Athena/Spark.
// Separate hot and cold storage... don't search S3 directly." This class is the
// ROUTER that decides which path a request takes, based on whether its requested
// time range reaches outside the hot window - callers never decide this
// themselves, so a search API caller doesn't need to know hot/cold storage exists
// at all.
public class LogSearchGateway {
    private final SearchService hotSearchService;
    private final ColdStorageQueryService coldQueryService;
    private final Duration hotRetention;

    public LogSearchGateway(SearchService hotSearchService, ColdStorageQueryService coldQueryService, Duration hotRetention) {
        this.hotSearchService = hotSearchService;
        this.coldQueryService = coldQueryService;
        this.hotRetention = hotRetention;
    }

    public List<LogEntry> search(SearchRequest request) {
        Instant hotCutoff = Instant.now().minus(hotRetention);
        boolean reachesIntoColdStorage = request.getFromTime() != null && request.getFromTime().isBefore(hotCutoff);

        if (!reachesIntoColdStorage) {
            return hotSearchService.search(request);
        }

        System.out.println("[LogSearchGateway] Request reaches beyond the " + hotRetention.toDays()
                + "-day hot window - routing to cold storage");
        return coldQueryService.scan(log ->
                (request.getServiceName() == null || log.getServiceName().equals(request.getServiceName()))
                        && (request.getLevel() == null || log.getLevel() == request.getLevel())
                        && (request.getFromTime() == null || !log.getTimestamp().isBefore(request.getFromTime()))
                        && (request.getToTime() == null || !log.getTimestamp().isAfter(request.getToTime())));
    }
}
