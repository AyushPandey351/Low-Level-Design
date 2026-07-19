package ringvideo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        VideoRepository videoRepository = new InMemoryVideoRepository();
        S3StorageService storageService = new S3StorageService();
        UploadService uploadService = new UploadService(videoRepository, storageService, Duration.ofDays(30));
        VideoDeduplicationService dedupService = new VideoDeduplicationService();

        Set<String> knownInfected = ConcurrentHashMap.newKeySet();
        VideoProcessingService processingService = new VideoProcessingService(
                List.of(new VirusScanProcessor(knownInfected), new ThumbnailProcessor(),
                        new TranscodingProcessor(), new MetadataExtractor()),
                videoRepository);

        BlockingQueue<Video> processingQueue = new LinkedBlockingQueue<>();
        List<VideoProcessingWorker> workers = new ArrayList<>();
        List<Thread> workerThreads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            VideoProcessingWorker worker = new VideoProcessingWorker(processingQueue, processingService);
            workers.add(worker);
            Thread t = new Thread(worker, "video-worker-" + i);
            t.start();
            workerThreads.add(t);
        }

        // The event-driven glue: an ObjectCreated notification looks up the Video by
        // s3Key, dedupes, marks it UPLOADED, and enqueues it for processing - this IS
        // "S3 -> Object Created Event -> Queue -> Processing Workers" from the notes.
        storageService.subscribeToObjectCreated(event -> {
            if (!dedupService.isNew(event.getS3Key())) {
                System.out.println("[Dedup] Ignoring duplicate completion event for " + event.getS3Key());
                return;
            }
            Video video = videoRepository.findByS3Key(event.getS3Key());
            video.setStatus(VideoStatus.UPLOADED);
            videoRepository.update(video);
            processingQueue.add(video);
        });

        ShareRepository shareRepository = new InMemoryShareRepository();
        ShareService shareService = new TokenShareService(shareRepository, Duration.ofDays(7));
        CdnService cdnService = new CdnService(storageService);
        RetentionService retentionService = new RetentionService(videoRepository, storageService);
        ReconciliationService reconciliationService = new ReconciliationService(videoRepository, storageService, processingQueue);

        // --- Basic upload flow ---
        System.out.println("== Camera cam001 uploads a clean 25MB clip ==");
        UploadResult upload1 = uploadService.createUpload("cam001", "home9", "ayush", 25_000_000);
        System.out.println("Got pre-signed URL: " + upload1.getUploadUrl());
        Video video1 = videoRepository.find(upload1.getVideoId());
        storageService.uploadObject(video1.getS3Key(), video1.getSize()); // simulates the camera's direct PUT
        Thread.sleep(300);
        System.out.println("Final status: " + videoRepository.find(video1.getVideoId()).getStatus()
                + ", thumbnail=" + videoRepository.find(video1.getVideoId()).getThumbnailKey());

        // --- Deduplication: the SAME object-created notification fires again ---
        System.out.println("\n== Duplicate completion event for the same object ==");
        storageService.uploadObject(video1.getS3Key(), video1.getSize());
        Thread.sleep(200);

        // --- Chain of Responsibility short-circuit: an infected upload ---
        System.out.println("\n== Camera cam001 uploads an INFECTED clip ==");
        UploadResult upload2 = uploadService.createUpload("cam001", "home9", "ayush", 10_000_000);
        knownInfected.add(upload2.getVideoId()); // registered BEFORE the object lands, so the scan sees it
        Video video2 = videoRepository.find(upload2.getVideoId());
        storageService.uploadObject(video2.getS3Key(), video2.getSize());
        Thread.sleep(300);
        Video video2After = videoRepository.find(upload2.getVideoId());
        System.out.println("Status: " + video2After.getStatus()
                + " (thumbnail must be null - the chain stopped before ThumbnailProcessor): "
                + video2After.getThumbnailKey());

        // --- Multipart resumable upload ---
        System.out.println("\n== Multipart upload: 4 parts, only 3 arrive, then resume the missing one ==");
        UploadSession session = uploadService.createMultipartUpload("cam001", "home9", "ayush", 80_000_000, 4);
        Video multipartVideo = videoRepository.find(session.getVideoId());
        storageService.uploadPart(multipartVideo.getS3Key(), 1);
        storageService.uploadPart(multipartVideo.getS3Key(), 2);
        storageService.uploadPart(multipartVideo.getS3Key(), 3);
        session.markPartComplete(1);
        session.markPartComplete(2);
        session.markPartComplete(3);
        System.out.println("Missing parts before resume: " + session.getMissingParts());
        System.out.println("Resuming - uploading ONLY the missing part, not restarting from part 1...");
        storageService.uploadPart(multipartVideo.getS3Key(), 4);
        session.markPartComplete(4);
        System.out.println("Session complete: " + session.isComplete());
        storageService.completeMultipartUpload(multipartVideo.getS3Key(), multipartVideo.getSize());
        session.markCompleted();
        Thread.sleep(300);
        System.out.println("Multipart video final status: " + videoRepository.find(multipartVideo.getVideoId()).getStatus());

        // --- Playback URL: before vs after transcoding, isolated from async timing ---
        System.out.println("\n== Playback URL before and after READY ==");
        Video playbackTest = new Video("VIDX", "cam001", "home9", "ayush", "videos/home9/VIDX.mp4",
                1_000_000, Instant.now(), Instant.now().plusSeconds(3600));
        playbackTest.setStatus(VideoStatus.PROCESSING);
        System.out.println("While PROCESSING: " + cdnService.getPlaybackUrl(playbackTest));
        playbackTest.setStatus(VideoStatus.READY);
        System.out.println("Once READY: " + cdnService.getPlaybackUrl(playbackTest));

        // --- Sharing ---
        System.out.println("\n== Sharing video1 ==");
        String shareLink = shareService.generateShareLink(video1.getVideoId(), "ayush");
        System.out.println("Share link: " + shareLink);
        String token = shareLink.substring(shareLink.lastIndexOf('/') + 1);
        System.out.println("Valid token check: " + shareService.validateToken(token));
        System.out.println("Bogus token check: " + shareService.validateToken("not-a-real-token"));

        // --- Retention / expiry ---
        System.out.println("\n== Retention: an already-expired video gets deleted ==");
        Video expiredVideo = new Video("VIDEXP", "cam001", "home9", "ayush", "videos/home9/VIDEXP.mp4",
                5_000_000, Instant.now().minusSeconds(3600 * 24 * 31), Instant.now().minusSeconds(60));
        videoRepository.save(expiredVideo);
        storageService.uploadObject(expiredVideo.getS3Key(), expiredVideo.getSize());
        Thread.sleep(200); // let it fan into processing so it's not confused with the reconciliation test below
        retentionService.runExpirySweep();
        System.out.println("Status: " + videoRepository.find("VIDEXP").getStatus()
                + ", object still in storage: " + storageService.exists(expiredVideo.getS3Key()));

        // --- Reconciliation: a lost S3 event ---
        System.out.println("\n== Reconciliation: object exists in storage but its ObjectCreatedEvent was lost ==");
        UploadResult lostEventUpload = uploadService.createUpload("cam001", "home9", "ayush", 12_000_000);
        Video lostEventVideo = videoRepository.find(lostEventUpload.getVideoId());
        storageService.simulateSilentUpload(lostEventVideo.getS3Key()); // object lands, but NO event fires
        System.out.println("Status before reconciliation: " + videoRepository.find(lostEventVideo.getVideoId()).getStatus());
        reconciliationService.run();
        Thread.sleep(300);
        System.out.println("Status after reconciliation: " + videoRepository.find(lostEventVideo.getVideoId()).getStatus());

        // --- Real concurrency test: many cameras uploading simultaneously ---
        System.out.println("\n== Concurrency: 40 cameras upload simultaneously ==");
        int uploadCount = 40;
        ExecutorService loadGenerator = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(uploadCount);
        List<String> bulkVideoIds = new java.util.concurrent.CopyOnWriteArrayList<>();
        for (int i = 0; i < uploadCount; i++) {
            int index = i;
            loadGenerator.submit(() -> {
                awaitLatch(startLatch);
                UploadResult result = uploadService.createUpload("bulkCam" + index, "bulkHome" + index, "user" + index, 5_000_000);
                bulkVideoIds.add(result.getVideoId());
                Video v = videoRepository.find(result.getVideoId());
                storageService.uploadObject(v.getS3Key(), v.getSize());
                doneLatch.countDown();
            });
        }
        startLatch.countDown();
        doneLatch.await();
        loadGenerator.shutdown();
        Thread.sleep(1500); // let the worker pool drain the burst

        long readyCount = bulkVideoIds.stream().filter(id -> videoRepository.find(id).getStatus() == VideoStatus.READY).count();
        System.out.println("Ready: " + readyCount + " out of " + uploadCount + " (must be " + uploadCount + ")");
        System.out.println("Remaining in processing queue: " + processingQueue.size() + " (must be 0)");

        for (VideoProcessingWorker worker : workers) {
            worker.stop();
        }
        for (Thread t : workerThreads) {
            t.join(500);
        }
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
