package ringvideo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryVideoRepository implements VideoRepository {
    private final Map<String, Video> videos = new ConcurrentHashMap<>();

    @Override
    public void save(Video video) {
        videos.put(video.getVideoId(), video);
    }

    @Override
    public Video find(String videoId) {
        return videos.get(videoId);
    }

    @Override
    public void update(Video video) {
        videos.put(video.getVideoId(), video);
    }

    @Override
    public List<Video> findByOwner(String ownerId) {
        return videos.values().stream()
                .filter(v -> v.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Video> findAll() {
        return List.copyOf(videos.values());
    }

    @Override
    public Video findByS3Key(String s3Key) {
        return videos.values().stream()
                .filter(v -> v.getS3Key().equals(s3Key))
                .findFirst()
                .orElse(null);
    }
}
