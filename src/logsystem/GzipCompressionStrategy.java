package logsystem;

public class GzipCompressionStrategy implements CompressionStrategy {
    @Override
    public int compress(int originalSizeBytes) {
        return (int) (originalSizeBytes * 0.30); // gzip on repetitive log text: strong ratio
    }

    @Override
    public String getName() {
        return "GZIP";
    }
}
