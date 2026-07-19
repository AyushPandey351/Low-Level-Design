package ringvideo;

public class ObjectCreatedEvent {
    private final String s3Key;
    private final long size;

    public ObjectCreatedEvent(String s3Key, long size) {
        this.s3Key = s3Key;
        this.size = size;
    }

    public String getS3Key() {
        return s3Key;
    }

    public long getSize() {
        return size;
    }
}
