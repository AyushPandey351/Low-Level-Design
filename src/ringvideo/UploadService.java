package ringvideo;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// The Steps 1-3 flow from the notes, made real: create a videoId + s3Key + Video
// metadata record (status=PENDING), ask ObjectStorageService for a pre-signed URL,
// and hand that URL back - WITHOUT ever touching the video bytes. The application
// server's job ends the moment it returns the URL; the camera's subsequent PUT goes
// straight to storage (S3StorageService.uploadObject in this simulation), and
// everything from there on is event-driven (see S3StorageService's
// ObjectCreatedEvent), not another call into this service.
public class UploadService {
    private final VideoRepository videoRepository;
    private final ObjectStorageService storageService;
    private final Duration retentionPeriod;
    private final AtomicInteger idCounter = new AtomicInteger();

    public UploadService(VideoRepository videoRepository, ObjectStorageService storageService, Duration retentionPeriod) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.retentionPeriod = retentionPeriod;
    }

    // Single pre-signed PUT - the common case for a normal-sized clip.
    public UploadResult createUpload(String deviceId, String homeId, String ownerId, long sizeBytes) {
        Video video = createPendingVideo(deviceId, homeId, ownerId, sizeBytes);
        return new UploadResult(video.getVideoId(), storageService.generateUploadUrl(video.getS3Key()));
    }

    // Multipart - Amazon follow-up #2 ("videos larger than 2GB"): split into
    // `partCount` chunks, hand back ONE pre-signed URL per chunk so the camera can
    // upload them independently (and in parallel, and out of order, and resume only
    // the missing ones later).
    public UploadSession createMultipartUpload(String deviceId, String homeId, String ownerId,
                                                long sizeBytes, int partCount) {
        Video video = createPendingVideo(deviceId, homeId, ownerId, sizeBytes);
        Map<Integer, String> partUrls = new HashMap<>();
        for (int part = 1; part <= partCount; part++) {
            partUrls.put(part, storageService.generateUploadUrl(video.getS3Key() + "#part" + part));
        }
        return new UploadSession("UPLOAD" + idCounter.incrementAndGet(), video.getVideoId(), partUrls);
    }

    private Video createPendingVideo(String deviceId, String homeId, String ownerId, long sizeBytes) {
        String videoId = "VID" + idCounter.incrementAndGet();
        String s3Key = "videos/" + homeId + "/" + videoId + ".mp4";
        Instant now = Instant.now();
        Video video = new Video(videoId, deviceId, homeId, ownerId, s3Key, sizeBytes, now, now.plus(retentionPeriod));
        videoRepository.save(video);
        return video;
    }
}
