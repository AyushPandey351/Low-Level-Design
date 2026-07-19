package ringvideo;

import java.time.Instant;

public class Share {
    private final String shareId;
    private final String videoId;
    private final String token;
    private final Instant expiry;
    private final String createdBy;

    public Share(String shareId, String videoId, String token, Instant expiry, String createdBy) {
        this.shareId = shareId;
        this.videoId = videoId;
        this.token = token;
        this.expiry = expiry;
        this.createdBy = createdBy;
    }

    public String getShareId() {
        return shareId;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
