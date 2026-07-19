package ringiot;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// Simulated Redis SETNX + TTL, same technique as RedisIdempotencyService in the Ring
// Notification Service design - ConcurrentHashMap.compute() gives atomic per-key
// check-and-set, and an entry older than `ttl` is treated as if it were never set,
// keeping the cache bounded (matching the notes' 24-hour TTL) instead of growing
// forever.
//
// Worth noting WHERE this runs in this design vs. the other Ring design: here,
// deduplication happens at INGESTION (DeviceIngestionService), before the event
// ever reaches Kafka - not inside a downstream consumer. That's a deliberate
// architectural difference, not an inconsistency: rejecting a duplicate here saves
// the ENTIRE pipeline (publish, partition fan-out to 3 consumer groups, processing)
// from doing wasted work, whereas checking only at the consumer (as the earlier Ring
// design did) tolerates a simpler ingestion path but wastes real downstream
// resources on duplicates that get filtered out only after they've already fanned out.
public class RedisEventDeduplicationService implements EventDeduplicationService {
    private final Map<String, Instant> seenAt = new ConcurrentHashMap<>();
    private final Duration ttl;

    public RedisEventDeduplicationService(Duration ttl) {
        this.ttl = ttl;
    }

    @Override
    public boolean trySetIfAbsent(String eventId) {
        AtomicBoolean firstToSet = new AtomicBoolean(false);
        seenAt.compute(eventId, (key, existing) -> {
            if (existing == null || isExpired(existing)) {
                firstToSet.set(true);
                return Instant.now();
            }
            return existing;
        });
        return firstToSet.get();
    }

    private boolean isExpired(Instant markedAt) {
        return Duration.between(markedAt, Instant.now()).compareTo(ttl) > 0;
    }
}
