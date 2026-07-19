package ringvideo;

// "Why CDN: without it, India -> US S3 is 300ms+; with the nearest edge, ~20ms."
// Modeled here simply as a URL rewrite (a real CDN would be a distinct
// infrastructure layer in front of ObjectStorageService, caching at edge
// locations) - the point being illustrated is WHICH url gets returned, not the
// actual edge-caching mechanics.
//
// getPlaybackUrl() concretely answers follow-up #3 ("how do you stream a video
// before transcoding completes"): a video that's still PROCESSING has no HLS/DASH
// renditions yet, so this falls back to serving the ORIGINAL uploaded file directly
// via a signed download URL, switching to the CDN-fronted, transcoded version only
// once the video reaches READY. The caller (playback API) doesn't need its own
// branching logic for this - it's encapsulated here.
public class CdnService {
    private final ObjectStorageService storageService;

    public CdnService(ObjectStorageService storageService) {
        this.storageService = storageService;
    }

    public String getPlaybackUrl(Video video) {
        if (video.getStatus() == VideoStatus.READY) {
            return "https://cdn.ring.com/" + video.getS3Key();
        }
        // Not yet transcoded - stream the original file directly instead of
        // waiting, matching the follow-up's explicit answer.
        return storageService.generateDownloadUrl(video.getS3Key());
    }
}
