package ring;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// Simulates "Redis with a TTL" - a real TTL (e.g. the notes' 24 hours) so the cache
// stays bounded rather than growing forever; a real Redis SETEX/EXPIRE does the same
// bounding. tryMarkProcessed() uses ConcurrentHashMap.compute(), which is atomic PER
// KEY - the same "atomically reserve, only the winner proceeds" idiom as
// PaymentRepository.reserveIdempotencyKey and NotificationRepository.markProcessedIfNew
// earlier in this series, adapted here to also treat an EXPIRED entry as if it were
// never marked at all (so a redelivery long after the TTL window is correctly
// treated as new, not permanently blocked).
public class RedisIdempotencyService implements IdempotencyService {
    private final Map<String, Instant> processedAt = new ConcurrentHashMap<>();
    private final Duration ttl;

    public RedisIdempotencyService(Duration ttl) {
        this.ttl = ttl;
    }

    @Override
    public boolean alreadyProcessed(String eventId) {
        Instant markedAt = processedAt.get(eventId);
        if (markedAt == null || isExpired(markedAt)) {
            return false;
        }
        return true;
    }

    @Override
    public void markProcessed(String eventId) {
        processedAt.put(eventId, Instant.now());
    }

    @Override
    public boolean tryMarkProcessed(String eventId) {
        AtomicBoolean firstToMark = new AtomicBoolean(false);
        processedAt.compute(eventId, (key, existing) -> {
            if (existing == null || isExpired(existing)) {
                firstToMark.set(true);
                return Instant.now();
            }
            return existing;
        });
        return firstToMark.get();
    }

    private boolean isExpired(Instant markedAt) {
        return Duration.between(markedAt, Instant.now()).compareTo(ttl) > 0;
    }
}
