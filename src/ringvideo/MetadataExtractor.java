package ringvideo;

// Last in the chain - extracts duration (simulated from file size, standing in for
// real container/codec inspection) and, since nothing downstream depends on
// anything further, marks the video READY. Playback (see CdnService) only starts
// serving through the CDN once a video reaches this status.
public class MetadataExtractor extends VideoProcessor {
    @Override
    protected boolean handle(Video video) {
        int estimatedSeconds = (int) Math.max(1, video.getSize() / (1024 * 1024));
        video.setDurationSeconds(estimatedSeconds);
        video.setStatus(VideoStatus.READY);
        System.out.println("[Metadata] " + video.getVideoId() + " duration=" + estimatedSeconds + "s, status=READY");
        return true;
    }
}
