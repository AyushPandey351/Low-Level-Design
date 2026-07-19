package logsystem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

// "Browser -> WebSocket -> Live Log Service -> Kafka Consumer." Standing in for the
// WebSocket layer with a plain in-process listener registry - a real deployment
// would push each onLog() invocation over an open WebSocket connection to the
// browser, but the mechanics that matter here (per-service subscription, fed by its
// OWN Kafka consumer group so live-tail latency is never coupled to how busy
// indexing or archiving happens to be) are the same either way.
public class LiveTailService {
    private final Map<String, List<Consumer<LogEntry>>> subscribersByService = new ConcurrentHashMap<>();

    public void subscribe(String serviceName, Consumer<LogEntry> subscriber) {
        subscribersByService.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    public void onLog(LogEntry log) {
        for (Consumer<LogEntry> subscriber : subscribersByService.getOrDefault(log.getServiceName(), List.of())) {
            subscriber.accept(log);
        }
    }
}
