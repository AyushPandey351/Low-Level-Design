package ringvideo;

// "Generate 1080p/720p/480p/360p - supports adaptive streaming (HLS/DASH)."
// Simulated as prints; a real implementation would invoke an actual transcoding
// pipeline per rendition.
public class TranscodingProcessor extends VideoProcessor {
    private static final String[] RENDITIONS = {"1080p", "720p", "480p", "360p"};

    @Override
    protected boolean handle(Video video) {
        for (String rendition : RENDITIONS) {
            System.out.println("[Transcode] " + video.getVideoId() + " -> " + rendition);
        }
        return true;
    }
}
