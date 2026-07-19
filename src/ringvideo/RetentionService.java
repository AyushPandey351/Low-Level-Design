package ringvideo;

import java.time.Instant;

// "Retention 30 days -> lifecycle rule -> S3 -> Delete. Metadata updated." A real
// deployment would use the storage provider's native lifecycle policies (e.g. S3
// Lifecycle Rules) rather than an application-level sweep for the ACTUAL deletion
// at scale - this class exists to make the metadata-side of that contract explicit
// and demonstrable: whenever the object is gone, Video.status must reflect EXPIRED,
// not silently point at a storage key that no longer exists.
public class RetentionService {
    private final VideoRepository videoRepository;
    private final ObjectStorageService storageService;

    public RetentionService(VideoRepository videoRepository, ObjectStorageService storageService) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
    }

    public void runExpirySweep() {
        Instant now = Instant.now();
        for (Video video : videoRepository.findAll()) {
            if (video.getStatus() != VideoStatus.EXPIRED && video.getExpiresAt().isBefore(now)) {
                storageService.deleteObject(video.getS3Key());
                video.setStatus(VideoStatus.EXPIRED);
                videoRepository.update(video);
                System.out.println("[Retention] Expired and deleted " + video.getVideoId());
            }
        }
    }
}
