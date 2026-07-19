package ringvideo;

import java.util.List;
import java.util.concurrent.BlockingQueue;

// "S3 Event Lost -> maintain a reconciliation job -> compare metadata vs S3 objects
// -> detect and repair inconsistencies." Finds videos stuck in PENDING whose object
// actually DOES exist in storage - meaning the ObjectCreatedEvent that should have
// advanced them was lost somewhere between S3 and the queue - and repairs the
// inconsistency by re-enqueueing them for processing, the same recovery path a
// successfully-delivered event would have taken.
public class ReconciliationService {
    private final VideoRepository videoRepository;
    private final ObjectStorageService storageService;
    private final BlockingQueue<Video> processingQueue;

    public ReconciliationService(VideoRepository videoRepository, ObjectStorageService storageService,
                                  BlockingQueue<Video> processingQueue) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.processingQueue = processingQueue;
    }

    public void run() {
        List<Video> stuck = videoRepository.findAll().stream()
                .filter(v -> v.getStatus() == VideoStatus.PENDING && storageService.exists(v.getS3Key()))
                .toList();

        for (Video video : stuck) {
            System.out.println("[Reconciliation] " + video.getVideoId()
                    + " has an object in storage but metadata never advanced - repairing by re-enqueueing");
            video.setStatus(VideoStatus.UPLOADED);
            videoRepository.update(video);
            processingQueue.add(video);
        }

        if (stuck.isEmpty()) {
            System.out.println("[Reconciliation] No inconsistencies found");
        }
    }
}
