package ringvideo;

import java.time.Instant;

// Metadata only, per the notes' own emphasis - "Only metadata. Never video itself."
// The actual bytes live in ObjectStorageService under s3Key; this class never holds
// them. Mutable fields (status, thumbnailKey, durationSeconds) are updated as the
// processing chain runs, so mutators are `synchronized` - same discipline as every
// stateful class in this series, since multiple processing workers could in theory
// touch related videos concurrently and this object is shared with the repository.
public class Video {
    private final String videoId;
    private final String deviceId;
    private final String homeId;
    private final String ownerId;
    private final String s3Key;
    private final long size;
    private VideoStatus status;
    private String thumbnailKey;
    private Integer durationSeconds;
    private final Instant createdAt;
    private final Instant expiresAt;

    public Video(String videoId, String deviceId, String homeId, String ownerId, String s3Key,
                 long size, Instant createdAt, Instant expiresAt) {
        this.videoId = videoId;
        this.deviceId = deviceId;
        this.homeId = homeId;
        this.ownerId = ownerId;
        this.s3Key = s3Key;
        this.size = size;
        this.status = VideoStatus.PENDING;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public synchronized void setStatus(VideoStatus status) {
        this.status = status;
    }

    public synchronized void setThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
    }

    public synchronized void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getHomeId() {
        return homeId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getS3Key() {
        return s3Key;
    }

    public long getSize() {
        return size;
    }

    public synchronized VideoStatus getStatus() {
        return status;
    }

    public synchronized String getThumbnailKey() {
        return thumbnailKey;
    }

    public synchronized Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return videoId + "[" + status + "]";
    }
}
