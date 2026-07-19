package ringvideo;

import java.util.Set;

// First in the chain, per the notes' pipeline order - deliberately first, since
// nothing downstream (thumbnail, transcode, metadata) should ever run against
// content that turns out to be malicious. Simulated via an explicit "known
// infected" set (a deterministic stand-in for a real scanning engine's verdict,
// since actual malware scanning is out of scope here) rather than a random/flaky
// outcome - a video's infection status is a FACT about that file, not something
// that flips between attempts.
public class VirusScanProcessor extends VideoProcessor {
    private final Set<String> knownInfectedVideoIds;

    public VirusScanProcessor(Set<String> knownInfectedVideoIds) {
        this.knownInfectedVideoIds = knownInfectedVideoIds;
    }

    @Override
    protected boolean handle(Video video) {
        if (knownInfectedVideoIds.contains(video.getVideoId())) {
            video.setStatus(VideoStatus.VIRUS_SCAN_FAILED);
            System.out.println("[VirusScan] " + video.getVideoId() + " FAILED scan - chain stopped here");
            return false; // stop the chain - nothing downstream should run
        }
        System.out.println("[VirusScan] " + video.getVideoId() + " clean");
        return true;
    }
}
