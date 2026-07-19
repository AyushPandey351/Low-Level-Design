package ringvideo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Tracks an upload before it's complete, per the notes. Extended beyond the notes'
// literal single preSignedUrl field to actually support multipart/resumable
// uploads (Amazon follow-up #2): `partUrls` holds one pre-signed URL per chunk, and
// `completedParts` tracks which chunks have actually landed - so a resumed upload
// can ask "which parts are still missing" and re-request ONLY those, rather than
// restarting the whole transfer. A ConcurrentHashMap-backed Set for completedParts
// because chunks can legitimately complete out of order and from parallel
// connections (the notes explicitly say "upload chunks in parallel").
public class UploadSession {
    private final String uploadId;
    private final String videoId;
    private final Map<Integer, String> partUrls;
    private final Set<Integer> completedParts = ConcurrentHashMap.newKeySet();
    private volatile UploadStatus status;

    public UploadSession(String uploadId, String videoId, Map<Integer, String> partUrls) {
        this.uploadId = uploadId;
        this.videoId = videoId;
        this.partUrls = partUrls;
        this.status = UploadStatus.INITIATED;
    }

    public void markPartComplete(int partNumber) {
        completedParts.add(partNumber);
        status = UploadStatus.IN_PROGRESS;
    }

    public boolean isComplete() {
        return completedParts.size() == partUrls.size();
    }

    public Set<Integer> getMissingParts() {
        return partUrls.keySet().stream()
                .filter(part -> !completedParts.contains(part))
                .collect(java.util.stream.Collectors.toSet());
    }

    public void markCompleted() {
        this.status = UploadStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = UploadStatus.FAILED;
    }

    public String getUploadId() {
        return uploadId;
    }

    public String getVideoId() {
        return videoId;
    }

    public Map<Integer, String> getPartUrls() {
        return partUrls;
    }

    public UploadStatus getStatus() {
        return status;
    }
}
