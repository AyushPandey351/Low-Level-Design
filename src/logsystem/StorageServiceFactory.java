package logsystem;

// Factory Pattern - "creates the appropriate indexer/storage implementation."
public class StorageServiceFactory {
    public static StorageService create(StorageBackend backend) {
        switch (backend) {
            case S3:
                return new S3ArchiveService();
            case LOCAL_DISK:
                return new LocalDiskArchiveService();
            default:
                throw new IllegalArgumentException("Unknown backend: " + backend);
        }
    }
}
