package ringvideo;

// Small return-value holder for UploadService.createUpload() - the caller needs
// BOTH the videoId (to reference the record afterward: check status, look it up,
// etc.) and the pre-signed URL (to actually hand to the camera), and a bare String
// can only carry one of those.
public class UploadResult {
    private final String videoId;
    private final String uploadUrl;

    public UploadResult(String videoId, String uploadUrl) {
        this.videoId = videoId;
        this.uploadUrl = uploadUrl;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }
}
