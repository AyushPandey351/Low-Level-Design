package ringvideo;

// Genuine Chain of Responsibility, not just a for-loop over a List - worth being
// explicit about the gap between the notes' code skeleton (VideoProcessingService
// iterating `List<VideoProcessor> processors`, unconditionally calling every one)
// and what the notes' OWN design-patterns table calls for ("Chain of
// Responsibility: sequential video processing pipeline"). A plain loop can't
// express the one behavior that actually matters here: if VirusScanProcessor finds
// malware, the pipeline must STOP - generating a thumbnail or transcoding an
// infected video makes no sense. That requires each handler to decide whether the
// NEXT handler runs at all, which is exactly what Chain of Responsibility is for
// and a flat loop structurally cannot express (a loop would either keep calling
// every processor regardless, or need an external "should I continue" flag checked
// between every iteration - at which point you've reinvented the chain anyway, just
// less clearly).
//
// `process()` is final and owns the "call handle(), then decide whether to
// continue" logic once, in the base class - subclasses only implement handle(),
// they can't get the chain-traversal part wrong.
public abstract class VideoProcessor {
    private VideoProcessor next;

    public VideoProcessor setNext(VideoProcessor next) {
        this.next = next;
        return next; // returned so callers can fluently chain: a.setNext(b).setNext(c)
    }

    public final void process(Video video) {
        boolean shouldContinue = handle(video);
        if (shouldContinue && next != null) {
            next.process(video);
        }
    }

    // Returns true to let the chain continue to the next processor, false to stop
    // it here (e.g. a failed virus scan).
    protected abstract boolean handle(Video video);
}
