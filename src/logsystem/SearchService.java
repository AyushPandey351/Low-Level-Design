package logsystem;

import java.util.List;

public interface SearchService {
    List<LogEntry> search(SearchRequest request);
}
