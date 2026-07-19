package ringvideo;

import java.util.List;

public interface VideoRepository {
    void save(Video video);

    Video find(String videoId);

    void update(Video video);

    // Not in the notes' literal 3-method interface - added because "GET
    // /users/{id}/videos" (history) needs some way to query by owner, and
    // Video already carries ownerId specifically to make this query possible.
    List<Video> findByOwner(String ownerId);

    List<Video> findAll();

    // ObjectCreatedEvent (see S3StorageService) only carries an s3Key, not a
    // videoId - this is what lets the event handler map "this object just landed"
    // back to the Video record it belongs to.
    Video findByS3Key(String s3Key);
}
