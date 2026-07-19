package ringvideo;

// Strategy Pattern, per the notes: "abstracts cloud storage... allows future
// implementations like Azure Blob or Google Cloud Storage." UploadService,
// CdnService, and RetentionService depend on THIS interface, never on
// S3StorageService directly.
//
// deleteObject() is included here (beyond the notes' literal two-method list)
// because RetentionService explicitly, deliberately calls it - "this video is
// expired, delete it now" is application-level business logic, not a storage
// provider's internal mechanism. That's different from uploadObject/uploadPart/
// event-firing (see S3StorageService), which stay OFF this interface because those
// really are provider-native side effects of receiving a PUT, not something the
// application explicitly invokes - the same distinction as "generateUploadUrl is a
// deliberate request" vs. "the object landing and the event firing just happen."
public interface ObjectStorageService {
    String generateUploadUrl(String key);

    String generateDownloadUrl(String key);

    void deleteObject(String key);

    // Same reasoning as deleteObject() - ReconciliationService deliberately asks
    // "does this object actually exist" as a real application-level check
    // (comparing metadata against storage to find inconsistencies), not something
    // that happens as a provider-internal side effect.
    boolean exists(String key);
}
