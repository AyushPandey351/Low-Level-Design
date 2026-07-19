package ringvideo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Simulated - no real HTTP PUT or AWS SDK call, same "ignore real third-party
// providers" treatment used for every external boundary in this series. What IS
// modeled faithfully: pre-signed URLs are just opaque tokens the camera would PUT
// to directly (the app never touches the bytes); uploadObject()/uploadPart() stand
// in for that direct PUT actually landing; and completing an object publishes an
// ObjectCreatedEvent, mirroring S3's real native event notification.
public class S3StorageService implements ObjectStorageService {
    private final Set<String> objects = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<Integer>> multipartProgress = new ConcurrentHashMap<>();
    private final EventPublisher<ObjectCreatedEvent> objectCreatedPublisher = new EventPublisher<>();

    @Override
    public String generateUploadUrl(String key) {
        return "https://storage.local/upload/" + key + "?sig=" + fakeSignature(key);
    }

    @Override
    public String generateDownloadUrl(String key) {
        return "https://storage.local/download/" + key + "?sig=" + fakeSignature(key);
    }

    public void subscribeToObjectCreated(java.util.function.Consumer<ObjectCreatedEvent> subscriber) {
        objectCreatedPublisher.subscribe(subscriber);
    }

    // Simulates a direct single-PUT upload landing - fires the event immediately.
    public void uploadObject(String key, long size) {
        objects.add(key);
        objectCreatedPublisher.publish(new ObjectCreatedEvent(key, size));
    }

    // Test-support only: simulates the object landing WITHOUT firing the
    // ObjectCreatedEvent - i.e. the exact "S3 Event Lost" scenario the notes
    // describe, where the object genuinely exists but the notification that should
    // have advanced its metadata never arrived. Lets Main demonstrate
    // ReconciliationService actually detecting and repairing that inconsistency.
    public void simulateSilentUpload(String key) {
        objects.add(key);
    }

    // Simulates ONE chunk of a multipart upload landing - does NOT fire the event
    // until every part for this key has arrived (see completeMultipartUpload).
    public void uploadPart(String key, int partNumber) {
        multipartProgress.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(partNumber);
    }

    public boolean isMultipartComplete(String key, int totalParts) {
        Set<Integer> parts = multipartProgress.get(key);
        return parts != null && parts.size() == totalParts;
    }

    public void completeMultipartUpload(String key, long totalSize) {
        objects.add(key);
        objectCreatedPublisher.publish(new ObjectCreatedEvent(key, totalSize));
    }

    @Override
    public boolean exists(String key) {
        return objects.contains(key);
    }

    // Backs the retention/lifecycle-policy flow: "Retention 30 days -> Lifecycle
    // rule -> S3 -> Delete." Part of ObjectStorageService (unlike uploadObject/
    // uploadPart above) since RetentionService calls this deliberately, through the
    // abstraction - see ObjectStorageService's comment on that distinction.
    @Override
    public void deleteObject(String key) {
        objects.remove(key);
    }

    private String fakeSignature(String key) {
        return Integer.toHexString(key.hashCode());
    }
}
