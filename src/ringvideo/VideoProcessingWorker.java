package ringvideo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

// Deliberately a SHARED queue with competing workers (SQS-style), NOT partitioned
// by any key - unlike the Ring Notification Service and Ring IoT Event Processing
// designs earlier in this series, video processing has no per-key ordering
// requirement. Each video's pipeline (virus scan -> thumbnail -> transcode ->
// metadata) is entirely independent of every other video's, even from the same
// camera - there's no equivalent of "motion must be processed before the doorbell
// press that came after it" here. So the simpler structure (one queue, N workers
// racing to grab whichever video is next) is the CORRECT fit, not a missed
// opportunity to partition.
public class VideoProcessingWorker implements Runnable {
    private final BlockingQueue<Video> queue;
    private final VideoProcessingService processingService;
    private volatile boolean running = true;

    public VideoProcessingWorker(BlockingQueue<Video> queue, VideoProcessingService processingService) {
        this.queue = queue;
        this.processingService = processingService;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Video video = queue.poll(200, TimeUnit.MILLISECONDS);
                if (video != null) {
                    processingService.process(video);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void stop() {
        running = false;
    }
}
