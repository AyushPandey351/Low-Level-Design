package logsystem;

public class SnappyCompressionStrategy implements CompressionStrategy {
    @Override
    public int compress(int originalSizeBytes) {
        return (int) (originalSizeBytes * 0.55); // snappy trades ratio for much lower CPU cost
    }

    @Override
    public String getName() {
        return "SNAPPY";
    }
}
