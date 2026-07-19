package ringvideo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// "Duplicate Uploads: use videoId or checksum/hash. Ignore duplicate completion
// events." - guards the ObjectCreatedEvent handler (see VideoProcessingWorker),
// keyed on s3Key, so a redelivered/duplicate completion notification for the SAME
// object can't queue the same video for processing twice. Set.add() on a
// ConcurrentHashMap-backed set is atomic - the same "first caller wins" idiom used
// for every dedup/idempotency check throughout this series.
public class VideoDeduplicationService {
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    public boolean isNew(String s3Key) {
        return processedKeys.add(s3Key);
    }
}
