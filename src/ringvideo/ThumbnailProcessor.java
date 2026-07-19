package ringvideo;

// "Creates cover.jpg, displayed in history - no need to download the entire
// video." Sets thumbnailKey on the shared Video record; the actual image
// generation is simulated, per the "ignore real processing" treatment applied to
// every external/heavy operation in this series.
public class ThumbnailProcessor extends VideoProcessor {
    @Override
    protected boolean handle(Video video) {
        String thumbnailKey = video.getS3Key().replace(".mp4", "-thumb.jpg");
        video.setThumbnailKey(thumbnailKey);
        System.out.println("[Thumbnail] generated " + thumbnailKey);
        return true;
    }
}
