package ringvideo;

import java.util.List;

// Wires the individual VideoProcessor instances into ONE chain (via setNext) and
// kicks it off - matching the notes' VideoProcessingService responsibility
// ("coordinates the processing pipeline") while internally using real
// Chain-of-Responsibility traversal instead of iterating a flat List, per
// VideoProcessor's comment on why that distinction matters for virus-scan
// short-circuiting.
public class VideoProcessingService {
    private final VideoProcessor chainHead;
    private final VideoRepository videoRepository;

    public VideoProcessingService(List<VideoProcessor> processorsInOrder, VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
        for (int i = 0; i < processorsInOrder.size() - 1; i++) {
            processorsInOrder.get(i).setNext(processorsInOrder.get(i + 1));
        }
        this.chainHead = processorsInOrder.get(0);
    }

    public void process(Video video) {
        video.setStatus(VideoStatus.PROCESSING);
        chainHead.process(video);
        videoRepository.update(video);
    }
}
