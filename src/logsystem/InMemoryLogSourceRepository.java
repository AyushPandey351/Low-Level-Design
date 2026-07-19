package logsystem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLogSourceRepository implements LogSourceRepository {
    private final Map<String, LogSource> sources = new ConcurrentHashMap<>();

    @Override
    public void save(LogSource source) {
        sources.put(source.getServiceId(), source);
    }

    @Override
    public LogSource find(String serviceId) {
        return sources.get(serviceId);
    }
}
